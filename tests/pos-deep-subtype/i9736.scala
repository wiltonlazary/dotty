//> using options -Xfatal-warnings

class Test {
  type MyCombo = Int | Unit
  val z: MyCombo = 10
   z match
    case i: Int => ???
    case _ => ???
}