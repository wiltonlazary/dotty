//> using options -Xfatal-warnings -Wimplausible-patterns
object UnitTest extends App {
  def foo(m: Unit) = m match {
    case runtime.BoxedUnit.UNIT => println("ok") // error
  }
  foo(())
}
