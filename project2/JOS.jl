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
    arguments::Vector{Symbol}
    methods::Vector{Method}
end

classes = Dict{Symbol, Class}()

function make_class(symb::Symbol, superclasses::Vector, slots::Vector)
    c = Class(symb, superclasses, slots)
    classes[symb] = c
    return c
end

macro defclass(symb, superclasses, slots...)
    dump(superclasses)
    symb2 = esc(symb)
    super = map(x -> classes[x], superclasses.args)
    return quote
        $symb2 = make_class(Symbol($("$symb")), $super, [$slots...])
    end
end

function make_instance(class::Class, mappings...)
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

function class_precedence_list(c::Class)
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
        if !(form1 in class_precedence_list(act1))
            return false
        end
    end
    return true
end

function sort_methods(g::GenericFunction, classes::Class...)
    compatible = filter(method -> is_compatible(method.types, classes), g.methods)

    sort(compatible, by=method ->
        map(pair -> tuple(findall(x -> x == pair[2], class_precedence_list(pair[1]))...)
           ,zip(classes, method.types)))
end

function best_method(g::GenericFunction, args::Class...)
    methods = sort_methods(g, args...)
    if methods == []
        error("ERROR: No applicable method")
    end
    return sort_methods(g, args...)[1]
end

macro defgeneric(expr)
    if isa(expr, Expr) && expr.head == :call
        name = esc(expr.args[1])
        args = expr.args[2:end]

        return :($(name) = GenericFunction($args, Vector()))
    else
        error("Syntax error: expression expected.")
    end
end

function (f::GenericFunction)(args...)
    return best_method(f, map(x -> x._class, args)...).func(args...)
end

macro defmethod(expr)
    if isa(expr, Expr) #&& expr.head == :=
        name = esc(expr.args[1].args[1])
        args = map(x -> x.args[1], expr.args[1].args[2:end])
        types = map(x -> classes[x.args[2]], expr.args[1].args[2:end]) # TODO check argument names/number
        lambda = :(($(args...),) -> $(expr.args[2]))
        return quote
            pushfirst!($name.methods, Method($types, $lambda))
        end
    else
        error("Syntax error: expression expected.")
    end
end

instanceof(x::Any, c::Class) = isa(x, Object) && c in class_precedence_list(x._class)

@defclass(C1, [], a)
@defclass(C2, [], b, c)
@defclass(C3, [C1, C2], d)

c3i1 = make_instance(C3, :a=>1, :b=>2, :c=>3, :d=>4)
c3i2 = make_instance(C3, :b=>2)

get_slot(c3i2, :b)
set_slot!(c3i2, :b, 3)
[get_slot(c3i1, s) for s in [:a, :b, :c]]

@defgeneric foo(c)
@defmethod foo(c::C1) = 1
@defmethod foo(c::C2) = c.b

foo(make_instance(C1))
foo(make_instance(C2, :b=>42))

# Multiple Dispatch Test
@defgeneric bar(x, y)
@defmethod bar(x::C1, y::C2) = x.a + y.b
@defmethod bar(x::C1, y::C3) = x.a - y.b
@defmethod bar(x::C3, y::C3) = x.a * y.b

c1i1 = make_instance(C1, :a=>1)
c2i1 = make_instance(C2, :b=>3)
c3i1 = make_instance(C3, :a=>1, :b=>2)
c3i2 = make_instance(C3, :b=>3, :a=>5)

bar(c1i1, c2i1)
bar(c2i1, c1i1)
bar(c1i1, c3i1)
bar(c3i1, c3i2)
