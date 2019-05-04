struct class
    name
    superclasses
    slots
    container
end

struct object
    class
    slots
end

function make_class(symb, superclasses, slots)
    container_type = Symbol("__internal_T_", symb)
    eval(quote mutable struct $container_type
            $(Expr(:block, slots...))
        end end)
    class(symb, superclasses, slots, container_type)
end

function make_instance(class::class, mappings...)
    inst = object(class, Dict())
    for m in mappings
        set_slot!(inst, m[1], m[2])
    end
    inst
end

function slot_exists(class::class, name::Symbol)
    if name in class.slots
        return true
    else
        for super in class.superclasses
            if slot_exists(super, name)
                return true
            end
        end
    end
    return false
end

function get_slot(instance::object, name::Symbol)
    if slot_exists(instance.class, name)
        if name in instance.slots
            return instance.slots[name]
        else
            error("ERROR: Slot ", name, " is unbound")
        end
    else
        error("ERROR: Slot ", name, " is missing")
    end
end

function set_slot!(instance::object, name::Symbol, value)
    if slot_exists(instance.class, name)
        instance.slots[name] = value
    else
        error("ERROR: Slot ", name, " is missing")
    end
end


C1 = make_class(:C1, [], [:a])
C2 = make_class(:C2, [], [:b, :c])
C3 = make_class(:C3, [C1, C2], [:d])

c3i1 = make_instance(C3, :a=>1, :b=>2, :c=>3, :d=>4)

c3i1.slots
