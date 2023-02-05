import scala.deriving.Mirror
import scala.reflect.ClassTag

type TupleUnionLub[T <: Tuple, Lub, Acc <: Lub] <: Lub = T match {
  case (h & Lub) *: t => TupleUnionLub[t, Lub, Acc | h]
  case EmptyTuple     => Acc
}

transparent inline given derived[A](
    using m: Mirror.SumOf[A],
    idClassTag: ClassTag[TupleUnionLub[m.MirroredElemTypes, A, Nothing]]
): Unit = ()

sealed trait Foo
case class FooA(a: Int) extends Foo

val instance = derived[Foo] // error