import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._
import scala.collection.mutable

@experimental
class newUnusedSymbol extends MacroAnnotation:
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case ClassDef(name, ctr, parents, self, body) =>
        val cls = tree.symbol
        val toStringMethType = Symbol.requiredMethod("java.lang.Object.toString").info
        val toStringOverrideSym = Symbol.newMethod(cls, "toString", toStringMethType, Flags.Override, Symbol.noSymbol)
        // Test that toStringOverrideSym is not accidentally entered in the class
        List(tree)
      case _ =>
        report.error("Annotation only supports `class`")
        List(tree)
