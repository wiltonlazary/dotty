trait Cap { def use(): Int }

def test1(io: Cap^{cap}): Unit = {
  type Id[X] = [T] -> (op: X ->{io} T) -> T

  val x: Id[Cap^{io}] = ???
  val f: (Cap^{cap}) -> Unit = ???
  x(f)  // ok
}

def test2(io: Cap^{cap}): Unit = {
  type Id[X] = [T] -> (op: X => T) -> T

  val x: Id[Cap^] = ???
  val f: Cap^{io} -> Unit = ???
  x(f)  // error
}
