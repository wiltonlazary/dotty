//> using options -Xfatal-warnings -Wunused:locals

val a = 1 // OK

var cs = 3 // OK

val b = // OK
  var e3 = 2 // error
  val e1 = 1 // error
  def e2 = 2 // error
  1

val c = // OK
  var e1 = 1 // error not set
  def e2 = e1 // OK
  val e3 = e2 // OK
    e3

val g = // OK
  var e1 = 1 // OK
  def e2 = e1 // OK
  val e3 = e2 // OK
  e1 = e3 // OK
    e3

def d = 1 // OK

def e = // OK
  val e1 = 1 // error
  def e2 = 2 // error
  var e3 = 4 // error
  1

def f = // OK
  val f1 = 1 // OK
  var f2 = f1 // error not set
  def f3 = f2 // OK
  f3

def h = // OK
  val f1 = 1 // OK
  var f2 = f1 // OK
  def f3 = f2 // OK
  f2 = f3 // OK
  f2

class Foo {
  val a = 1 // OK

  var cs = 3 // OK

  val b = // OK
    var e3 = 2 // error
    val e1 = 1 // error
    def e2 = 2 // error
    1

  val c = // OK
    var e1 = 1 // error not set
    def e2 = e1 // OK
    val e3 = e2 // OK
    e3

  val g = // OK
    var e1 = 1 // OK
    def e2 = e1 // OK
    val e3 = e2 // OK
    e1 = e3 // OK
    e3

  def d = 1 // OK

  def e = // OK
    val e1 = 1 // error
    def e2 = 2 // error
    var e3 = 4 // error
    1

  def f = // OK
    val f1 = 1 // OK
    var f2 = f1 // error not set
    def f3 = f2 // OK
    f3

  def h = // OK
    val f1 = 1 // OK
    var f2 = f1 // OK
    def f3 = f2 // OK
    f2 = f3 // OK
    f2
}

// ---- SCALA 2 tests ----

package foo.scala2.tests:
  class Outer {
    class Inner
  }

  trait Locals {
    def f0 = {
      var x = 1 // error
      var y = 2 // OK
      y = 3
      y + y
    }
    def f1 = {
      val a = new Outer // OK
      val b = new Outer // error
      new a.Inner
    }
    def f2 = {
      var x = 100 // error not set
      x
    }
  }

  object Types {
    def l1() = {
      object HiObject { def f = this } // OK
      class Hi { // error
        def f1: Hi = new Hi
        def f2(x: Hi) = x
      }
      class DingDongDoobie // error
      class Bippy // OK
      type Something = Bippy // OK
      type OtherThing = String // error
      (new Bippy): Something
    }
  }

package test.foo.twisted.i16682:
  def myPackage =
    object IntExtractor: // OK
      def unapply(s: String): Option[Int] = s.toIntOption

    def isInt(s: String) = s match { // OK
      case IntExtractor(i) => println(s"Number $i")
      case _ => println("NaN")
    }
    isInt

  def f = myPackage("42")
