mutable struct Object
    _class::Union{Missing, Object}
    _slots::Dict{Symbol, Any}
end

struct Method
    types::Vector{Object}
    func::Function
end

struct GenericFunction
    parameters::Vector{Symbol}
    methods::Vector{Method}
end

# ==================== CLASSES ====================

classes = Dict{Symbol, Object}()

function get_class(symb::Symbol)
    haskey(classes, symb) ? classes[symb] : error("ERROR: Unknown class ", symb)
end

function make_class(meta::Object, symb::Symbol, superclasses::Vector, slots::Vector)
    c = make_instance(meta, :name => symb, :superclasses => superclasses, :slots => slots)
    classes[symb] = c
    return c
end

macro defclass(meta, symb, superclasses, slots...)
    meta = get_class(meta)
    symb2 = esc(symb)
    super = map(x -> get_class(x), superclasses.args)
    return quote
        $symb2 = make_class($meta, Symbol($("$symb")), $super, [$slots...])
    end
end



# ==================== OBJECTS ====================

function make_instance(class::Object, mappings::Pair...)
    inst = Object(class, Dict())
    for m in mappings
        set_slot!(inst, m[1], m[2])
    end
    return inst
end

function slot_exists(class::Object, name::Symbol, visited=[])
    if name == :slots || name == :superclasses || name in class.slots
        return true
    else
        for super in class.superclasses #DFS
            if !(super in visited)
                if slot_exists(super, name, push!(visited, class))
                    return true
                end
            end
        end
    end
    return false
end

function get_slot(instance::Object, name::Symbol)
    if slot_exists(instance._class, name)
        if haskey(instance._slots, name)
            return instance._slots[name]
        else
            error("ERROR: Slot ", name, " is unbound")
        end
    else
        error("ERROR: Slot ", name, " is missing")
    end
end

function Base.getproperty(instance::Object, name::Symbol)
    if name === :_slots || name === :_class
        return getfield(instance, name)
    else
        return get_slot(instance, name)
    end
end


function set_slot!(instance::Object, name::Symbol, value)
    if slot_exists(instance._class, name)
        instance._slots[name] = value
    else
        error("ERROR: Slot ", name, " is missing")
    end
end


function Base.setproperty!(instance::Object, name::Symbol, value)
    if name === :_slots || name === :_class
        return setfield!(instance, name, value)
    else
        return set_slot!(instance, name, value)
    end
end

instanceof(x::Any, c::Object) = false
instanceof(x::Object, c::Object) = c in collect_classes(x._class)

# ==================== FUNCTIONS ====================

macro defgeneric(expr)
    if isa(expr, Expr) && expr.head == :call
        name = esc(expr.args[1])
        params = expr.args[2:end]

        return :($(name) = GenericFunction($params, Vector()))
    else
        error("Syntax error: expression expected.")
    end
end

function (f::GenericFunction)(args...)
    return best_method(f, map(x -> wrap(x)._class, args)...).func(args...)
end

# ==================== METHODS ====================

macro defmethod(expr)
    if isa(expr, Expr) #&& expr.head == :=
        name = esc(expr.args[1].args[1])
        args = map(x -> x.args[1], expr.args[1].args[2:end])
        lambda = :(($(args...),) -> $(expr.args[2]))
        return quote
            if $args != $name.parameters
                error("ERROR: method parameters do not match function definition.")
            else
                types = $(map(x -> get_class(x.args[2]), expr.args[1].args[2:end]))
                pushfirst!($name.methods, Method(types, $lambda))
            end
        end
    else
        error("Syntax error: expression expected.")
    end
end

function best_method(g::GenericFunction, args::Object...)
    methods = sort_methods(g, args...)
    if methods == []
        error("ERROR: No applicable method")
    end
    return methods[1]
end

function sort_methods(g::GenericFunction, classes::Object...)
    compatible = filter(method -> is_compatible(method.types, classes), g.methods)

    sort(compatible, by=method ->
        map(pair -> tuple(findall(x -> x == pair[2], class_precedence_list(pair[1]))...)
           ,zip(classes, method.types)))
end

function collect_classes(c::Object)
    discovered = []
    S = [c]
    while S != []
        v = pop!(S)
        if !(v in discovered)
            push!(discovered, v)
            for super in reverse(v.superclasses)
                push!(S, super)
            end
        end
    end
    return discovered
end

function is_compatible(formal, actual)
    for (form1, act1) in zip(formal, actual)
        if !(form1 in collect_classes(act1))
            return false
        end
    end
    return true
end

# ==================== META ====================

# we need to manually construct the first classes
class = Object(missing, Dict(:name => :class, :superclasses => [], :slots => [:name, :superclasses, :slots]))
standard_class = Object(missing, Dict(:name => :standard_class, :superclasses => [class], :slots => []))
class._class = standard_class
standard_class._class = standard_class
classes[:class] = class
classes[:standard_class] = standard_class

@defclass(standard_class, loops_class, [class])

@defgeneric class_precedence_list(c)

@defmethod class_precedence_list(c::standard_class) = begin
    discovered = []
    S = [c]
    while S != []
        v = pop!(S)
        if !(v in discovered)
            push!(discovered, v)
            for super in reverse(v.superclasses)
                push!(S, super)
            end
        end
    end
    return discovered
end

@defmethod class_precedence_list(c::loops_class) = begin
    discovered = []
    S = [c]
    while S != []
        v = pop!(S)
        if !(v in discovered)
            push!(discovered, v)
            for super in reverse(v.superclasses)
                push!(S, super)
            end
        else
            deleteat!(discovered, indexin([v], discovered))
            push!(discovered, v)
        end
    end
    return discovered
end

# ==================== TYPES ====================

wrap(x::Any)             = error("Invalid native value of type ", typeof(x))
wrap(x::Integer)         = make_instance(int, :value=>x)
wrap(x::AbstractFloat)   = make_instance(float, :value=>x)
wrap(x::String)          = make_instance(string, :value=>x)
wrap(x::GenericFunction) = make_instance(generic_function, :value=>x)
wrap(x::Tuple)           = make_instance(tpl, :value=>x)
wrap(x::Object)          = x

@defclass(standard_class, native_wrapper, [], value)
@defclass(standard_class, generic_function, [native_wrapper])
@defclass(standard_class, tpl, [native_wrapper])

@defclass(standard_class, number, [native_wrapper])
@defclass(standard_class, real, [number])
@defclass(standard_class, int, [real])
@defclass(standard_class, float, [real])
@defclass(standard_class, string, [native_wrapper])

@defgeneric len(x)
@defmethod len(x::string) = length(x)
@defmethod len(x::number) = "What is the lenght of a number??"
