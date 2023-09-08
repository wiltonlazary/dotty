// https://github.com/lampepfl/dotty/issues/8300

type Bar[X] = X match {
  case List[a] => List[Tuple1[a]]
  case Set[a]  => Set[Tuple1[a]]
}

object Test:
  (Set(1, 2, 3), List("a", "b")).map(
    [A] =>
      (a: A) =>
        a match {
          case it: Iterable[x] => it.map(Tuple1(_)).asInstanceOf[Bar[A]]
      }
  )
