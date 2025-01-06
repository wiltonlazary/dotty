//> using options -experimental

import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._

@experimental
class foo extends MacroAnnotation {
  def transform(using Quotes)(definition: quotes.reflect.Definition, companion: Option[quotes.reflect.Definition]): List[quotes.reflect.Definition] =
    import quotes.reflect._
    definition match
      case DefDef(name, params, tpt, Some(t)) =>
        given Quotes = definition.symbol.asQuotes
        val rhs = '{
          @hello def foo(x: Int): Int = x + 1
          ${t.asExprOf[Int]}
        }.asTerm
        val newDef = DefDef.copy(definition)(name, params, tpt, Some(rhs))
        List(newDef)
}
