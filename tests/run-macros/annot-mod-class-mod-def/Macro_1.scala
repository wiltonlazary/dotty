import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._
import scala.collection.mutable

@experimental
class modToString(msg: String) extends MacroAnnotation:
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case ClassDef(name, ctr, parents, self, body) =>
        val cls = tree.symbol
        val toStringSym = cls.methodMember("toString").head

        val newBody = body.span(_.symbol != toStringSym) match
          case (before, toStringDef :: after) =>
            val newToStringDef = DefDef(toStringDef.symbol, _ => Some(Literal(StringConstant(msg))))
            before ::: newToStringDef :: after
          case _ =>
            report.error("toString was not defined")
            body

        List(ClassDef.copy(tree)(name, ctr, parents, self, newBody))
      case _ =>
        report.error("Annotation only supports `class`")
        List(tree)
