/* Using `valueOf` is a way to check that the Java string literals were properly
 * parsed, since the parsed value is what the Scala compiler will use when
 * resolving the singleton types
 */
object Test extends App {
  println("====")
  println(valueOf[TextBlocks.aText.type])
  println("====")
  println(valueOf[TextBlocks.html1.type])
  println("====")
  println(valueOf[TextBlocks.query.type])
  println("====")
  println(valueOf[TextBlocks.html2.type])
  println("====")
  println(valueOf[TextBlocks.html3.type])
  println("====")
  println(valueOf[TextBlocks.html4.type])
  println("====")
  println(valueOf[TextBlocks.html5.type])
  println("====")
  println(valueOf[TextBlocks.mixedIndents.type])
  println("====")
  println(valueOf[TextBlocks.code.type])
  println("====")
  println(valueOf[TextBlocks.simpleString.type])
  println("====")
  println(valueOf[TextBlocks.emptyString.type])
  println("====")
  println(valueOf[TextBlocks.XY.type])
  println("====")
  println(valueOf[TextBlocks.Octal.type])
  println("====")
}
