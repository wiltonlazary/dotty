object A {
  def a: Int =
    B
      .s.length // error
}

object B {
  val s: String = s"${A.a}a"
}
