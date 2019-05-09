struct class
    name::Symbol
    superclasses::Vector{class}
    slots::Vector{Symbol}
end

struct object
    _class::class
    _slots::Dict{Symbol, Any}
end

struct method
    name::Symbol
    types::Vector{class}
    func::Function
end

struct generic
    name::Symbol
    arguments::Vector{Symbol}
    methods::Vector{method}
end

generics = Dict{Symbol, generic}()
classes = Dict{Symbol, class}()

function make_class(symb::Symbol, superclasses, slots)
    c = class(symb, superclasses, slots)
    classes[symb] = c
    return c
end

function make_instance(class::class, mappings...)
    inst = object(class, Dict())
    for m in mappings
        set_slot!(inst, m[1], m[2])
    end
    return inst
end

function slot_exists(class::class, name::Symbol)
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

function get_slot(instance::object, name::Symbol)
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

function Base.getproperty(instance::object, name::Symbol)
    if name === :_slots || name === :_class
        return getfield(instance, name)
    else
        return get_slot(instance, name)
    end
end

function set_slot!(instance::object, name::Symbol, value)
    if slot_exists(instance._class, name)
        instance._slots[name] = value
    else
        error("ERROR: Slot ", name, " is missing")
    end
end

function move_up(name::Symbol, args::class...)
    args = [args...]
    for i in 1 : size(args)[1]
        temp = args[i]
        for super in args[i].superclasses
            args[i] = super
            m = best_method(name, args...)
            if m != false
                return m
            end
        end
        args[i] = temp
    end
    return false
end

function best_method(name::Symbol, args::class...)
    #println("Trying ", name, " with ", args)
    for method in generics[name].methods
        found = true
        for (method_t, actual_t) in zip(method.types, args)
            if method_t != actual_t
                found = false
                break
            end
        end
        if found
            return method
        end
    end
    return move_up(name, args...)
end

function funcall(name::Symbol, args::object...)
    if haskey(generics, name)
        m = best_method(name, map(x -> x._class, args)...)
        if isa(m, method)
            return m.func(args...)
        else
            error("ERROR: No applicable method")
        end
    else
        error("Unkown generic function: ", name)
    end
end

macro defgeneric(expr)
    if isa(expr, Expr) && expr.head == :call
        name = expr.args[1]
        args = expr.args[2:end]
        gen = generic(name, args, Vector())
        generics[name] = gen

        return :($(name)($(args...)) = funcall(Symbol($name), $(args...)))
    else
        error("Syntax error: expression expected.")
    end
end

macro defmethod(expr)
    if isa(expr, Expr) #&& expr.head == :=
        name = expr.args[1].args[1]
        args = map(x -> x.args[1], expr.args[1].args[2:end])
        types = map(x -> classes[x.args[2]], expr.args[1].args[2:end]) # TODO check argument names/number
        lambda = :(($(args...),) -> $(expr.args[2]))
        return quote
            pushfirst!(generics[Symbol($name)].methods, method(Symbol($name), $types, $lambda))
        end
    else
        error("Syntax error: expression expected.")
    end
end

C1 = make_class(:C1, [], [:a])
C2 = make_class(:C2, [], [:b, :c])
C3 = make_class(:C3, [C1, C2], [:d])

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
