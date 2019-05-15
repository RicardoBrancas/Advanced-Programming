struct Class
    name::Symbol
    superclasses::Vector{Class}
    slots::Vector{Symbol}
end

struct Object
    _class::Class
    _slots::Dict{Symbol, Any}
end

struct Method
    types::Vector{Class}
    func::Function
end

struct GenericFunction
    parameters::Vector{Symbol}
    methods::Vector{Method}
end

# ==================== CLASSES ====================

classes = Dict{Symbol, Class}()

function get_class(symb::Symbol)
    haskey(classes, symb) ? classes[symb] : error("Unknown class ", symb)
end

function make_class(symb::Symbol, superclasses::Vector, slots::Vector)
    c = Class(symb, superclasses, slots)
    classes[symb] = c
    return c
end

macro defclass(symb, superclasses, slots...)
    dump(superclasses)
    symb2 = esc(symb)
    super = map(x -> get_class(x), superclasses.args)
    return quote
        $symb2 = make_class(Symbol($("$symb")), $super, [$slots...])
    end
end

function class_precedence_list(c::Class) #DFS, removing duplicates found later. ie Flavors
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

# ==================== OBJECTS ====================

function make_instance(class::Class, mappings::Pair...)
    inst = Object(class, Dict())
    for m in mappings
        set_slot!(inst, m[1], m[2])
    end
    return inst
end

function slot_exists(class::Class, name::Symbol)
    if name in class.slots
        return true
    else
        for super in class.superclasses #DFS
            if slot_exists(super, name)
                return true
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
            error("Slot ", name, " is unbound")
        end
    else
        error("Slot ", name, " is missing")
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
        error("Slot ", name, " is missing")
    end
end
function Base.setproperty!(instance::Object, name::Symbol, value)
    if name === :_slots || name === :_class
        error("ERROR: Can't assign slots or class name")
    else
        return set_slot!(instance, name, value)
    end
end

instanceof(x::Any, c::Class) = false
instanceof(x::Object, c::Class) = c in class_precedence_list(x._class)

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
    return best_method(f, map(x -> x._class, args)...).func(args...)
end

# ==================== METHODS ====================

macro defmethod(expr)
    if isa(expr, Expr) #&& expr.head == :=
        name = esc(expr.args[1].args[1])
        args = map(x -> x.args[1], expr.args[1].args[2:end])
        lambda = :(($(args...),) -> $(expr.args[2]))
        return quote
            if $args != $name.parameters
                error("Method parameters do not match function definition.")
            else
                types = $(map(x -> get_class(x.args[2]), expr.args[1].args[2:end]))
                pushfirst!($name.methods, Method(types, $lambda))
            end
        end
    else
        error("Syntax error: expression expected.")
    end
end

function best_method(g::GenericFunction, args::Class...)
    methods = sort_methods(g, args...)
    if methods == []
        error("No applicable method")
    end
    return methods[1]
end

function sort_methods(g::GenericFunction, classes::Class...)
    compatible = filter(method -> is_compatible(method.types, classes), g.methods)

    sort(compatible, by=method ->
        map(pair -> tuple(findall(x -> x == pair[2], class_precedence_list(pair[1]))...)
           ,zip(classes, method.types)))
end

function is_compatible(formal, actual)
    for (form1, act1) in zip(formal, actual)
        if !(form1 in class_precedence_list(act1))
            return false
        end
    end
    return true
end
