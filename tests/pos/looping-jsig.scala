//> using options -source 3.2

import scala.collection.mutable.*

trait BugTrack {
    trait B[+T]
    val cache : HashMap[A[_], B[_]] = HashMap.empty

    def A[T](f: Int => B[T]): A[T]
        = new A[T]{def apply(in: Int) = f(in)}

    abstract class A[+T] extends (Int => B[T]) {
      def giveMeSame = this
    }

    def amethod[T](p: =>A[T]): A[T] = A(in => cache.get(p) match {
           case Some(res) => res
           case None => p(in)
     }).giveMeSame.asInstanceOf[A[T]]
}
