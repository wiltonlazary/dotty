//> using options -Xfatal-warnings

import quoted.*

def foo(using Quotes)(x: Expr[Int]) =
  val '{ ($y: Int) + ($z: Int) } = x // error
  val '{ $a: Int } = x
  val '{ $b: Any } = x
  val '{ $c } = x
