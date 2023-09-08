
trait Iterable[+A]

/** Base trait for instances that can construct a collection from an iterable */
trait FromIterable {
  type C[X] <: Iterable[X]^
  def fromIterable[B](it: Iterable[B]^): C[B]^{it}
  def empty[A]: C[A] = fromIterable(??? : Iterable[A])
}
