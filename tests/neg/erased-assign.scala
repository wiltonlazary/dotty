//> using options -language:experimental.erasedDefinitions

object Test {
  var i: Int = 1
  def foo(erased a: Int): Int = {
    i = a // error
    erased def r = {
      i = a
      ()
    }
    42
  }
}
