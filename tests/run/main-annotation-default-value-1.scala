// scalajs: --skip

import scala.annotation.newMain

// Sample main method
object myProgram:

  /** Adds two numbers */
  @newMain def add(num: Int = 0, inc: Int = 1): Unit =
    println(s"$num + $inc = ${num + inc}")

end myProgram

object Test:
  def callMain(args: Array[String]): Unit =
    val clazz = Class.forName("add")
    val method = clazz.getMethod("main", classOf[Array[String]])
    method.invoke(null, args)

  def main(args: Array[String]): Unit =
    callMain(Array("2", "3"))
    callMain(Array("2"))
    callMain(Array())
end Test
