//> using options -Yno-experimental

import scala.annotation.experimental

@experimental
transparent inline def g() = ()

def test: Unit =
  g() // error
  ()
