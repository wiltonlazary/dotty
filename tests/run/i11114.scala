// scalajs: --skip

@main def Test() =
  println((1, 1).getClass)
  println(Tuple2.apply(1, 1).getClass)
  println(new Tuple2(1, 1).getClass)

  import Tuple2.apply
  println(apply(1, 4).getClass)

  import Tuple2.{ apply => t2 }
  println(t2(1, 5).getClass)

  println({ println("initialise"); Tuple2 }.apply(1, 6).getClass)
