// scalac: -Werror
object Main extends App:
   enum Extends[A, B]:
     case Ev[B, A <: B]() extends (A Extends B)

     def cast(a: A): B = this match {
       case Extends.Ev() => a
     }
