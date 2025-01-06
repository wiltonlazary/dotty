trait CmdLineParser:
  outer =>

  val a: String

  trait Opt[+T]:
    val default: T
    val names: Set[String]
    val help: String

  trait IntOpt extends Opt[Int]:
    println("outer = " + outer)
    println("outer.a = " + outer.a)

object FirstParser extends CmdLineParser:
  object OptMinSuccess extends IntOpt:  // warn
    val default = 100
    val names = Set("bla")
    val help = "bla"

  val opts = Some(OptMinSuccess)
  val a = "FirstParser"
