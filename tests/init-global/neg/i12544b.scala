enum Enum:
  case Case

object Enum:
  object nested:              // error
    val a: Enum = Case

  val b: Enum = f(nested.a)   // error

  def f(e: Enum): Enum = e

@main def main(): Unit = println(Enum.b)
