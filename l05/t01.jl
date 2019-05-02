square(x) = x * x

square(square(square(123456789)))
square(square(square(big(123456789))))
square(1.5)
square("Hi")
*(2, 3)

function square(x)
    println("Square being computed")
    x*x
end

square(x) =
    begin
        println("Square being computed")
        x*x
    end

square(x) = (println("Square being computed"); x*x)

fact(n) =
    n == 0 ?  1 : n *  fact(n-1)

fact(n::String) = "Don't even try"

fact("10")


t = (2, 1, 3)

x, y, z = t

t[end-1]

for e in t
    println(e)
end

a = Any[2, 1, 3]
a[1] = 9.2

a

abstract type Person end
abstract type YoungPerson <: Person end

struct Student <: YoungPerson
    name::String
    age::Int
end

s = Student("John", 25)

s.name
getfield(s, :name)

struct Point{T} where T <: Number
    x::T
    y::T
    z::T
end
