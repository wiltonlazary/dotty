package advanced

import scala.language.higherKinds
import scala.language.reflectiveCalls

import scala.reflect.Selectable.reflectiveSelectable

class C[T] {
  def t: T = ???
}

class Structural {
  def s1: { val x: Int } = ???
  def s2: { val x: Int } = new { val x: Int = ??? }
  def s3: { def m(x: Int): Int } = new { def m(x: Int): Int = ??? }
  def s4(a: Int): { val x: Int } = ???
  trait T[A] { val foo: { type B = A } = ???; def bar(b: foo.B) = () } // from tests/pos/t8177e.scala
}

class Wildcards {
  def e1: List[_] = ???
  def e2: List[_ <: Int] = ???
}

object Test {
  val s = new Structural
  val s1 = s.s1
  val s1x = s.s1.x
  val s2 = s.s2
  val s2x = s.s2.x
  val s3 = s.s3
  val s3x = s.s3.m(???)

  val e = new Wildcards
  val e1 = e.e1
  val e1x = e.e1.head

  {
    (??? : Any) match {
      case e3: List[_] =>
        val e3x = e3.head
        ()
    }
  }

  // see: https://github.com/scala/scala3/pull/14608#discussion_r835642563
  lazy val foo: (reflect.Selectable { type A = Int }) & (reflect.Selectable { type A = Int; val a: A }) = ???
  def bar: foo.A = foo.a
}


// Curried Type Application
class HKClass[F <: [T] =>> [U] =>> (U, T)] {
  def foo[T,U](x: F[T][U]): String = x.toString()
}
