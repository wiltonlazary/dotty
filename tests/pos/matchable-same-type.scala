//> using options -Xfatal-warnings -deprecation -feature

import scala.language.`future-migration`

type X
def x: X = ???
def test: Unit =
  x match
    case y: X =>
