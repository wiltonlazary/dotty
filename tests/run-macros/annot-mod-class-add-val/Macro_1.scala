import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._
import scala.collection.mutable

@experimental
class addMemoToString(msg: String) extends MacroAnnotation:
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case ClassDef(name, ctr, parents, self, body) =>
        val cls = tree.symbol
        val stringValSym = Symbol.newVal(cls, Symbol.freshName("string"), TypeRepr.of[String], Flags.Private, Symbol.noSymbol)

        val toStringMethType = Symbol.requiredMethod("java.lang.Object.toString").info
        val toStringOverrideSym = Symbol.newMethod(cls, "toString", toStringMethType, Flags.Override, Symbol.noSymbol)

        val stringValDef = ValDef(stringValSym, Some(Literal(StringConstant(msg))))
        val toStringDef = DefDef(toStringOverrideSym, _ => Some(Ref(stringValSym)))

        val newClassDef = ClassDef.copy(tree)(name, ctr, parents, self, stringValDef :: toStringDef :: body)
        List(newClassDef)

      case _ =>
        report.error("Annotation only supports `class`")
        List(tree)
