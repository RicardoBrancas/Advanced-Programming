struct class
    name
    superclasses
    slots
end

struct object
    class
    slots
end

function make_class(symb::Symbol, superclasses, slots)
    class(symb, superclasses, slots)
end

macro defclass(symb, superclasses, slots...)
    :(make_class($symb, $superclasses, $slots))
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
        if haskey(instance.slots, name)
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


:(C1 = make_class(:C1, [], [:a]))
C2 = make_class(:C2, [], [:b, :c])
C3 = make_class(:C3, [C1, C2], [:d])

@defclass(C4, [], a)

c3i1 = make_instance(C3, :a=>1, :b=>2, :c=>3, :d=>4)
c3i2 = make_instance(C3, :b=>2)

get_slot(c3i2, :b)
set_slot!(c3i2, :b, 3)
println([get_slot(c3i1, s) for s in [:a, :b, :c]])
