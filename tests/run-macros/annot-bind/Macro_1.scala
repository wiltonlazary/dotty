import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._

@experimental
class bind(str: String) extends MacroAnnotation:
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case ValDef(name, tpt, Some(rhsTree)) =>
        val valSym = Symbol.newVal(Symbol.spliceOwner, Symbol.freshName(str), tpt.tpe, Flags.Private, Symbol.noSymbol)
        val valDef = ValDef(valSym, Some(rhsTree))
        val newRhs = Ref(valSym)
        val newTree = ValDef.copy(tree)(name, tpt, Some(newRhs))
        List(valDef, newTree)
      case _ =>
        report.error("Annotation only supported on `val` with a single argument are supported")
        List(tree)
