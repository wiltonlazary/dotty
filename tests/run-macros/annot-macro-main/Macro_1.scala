import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._
import scala.collection.mutable

@experimental
class mainMacro extends MacroAnnotation:
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case DefDef(name, List(TermParamClause(Nil)), _, _) =>
        val parents = List(TypeTree.of[Object])
        def decls(cls: Symbol): List[Symbol] =
          List(Symbol.newMethod(cls, "main", MethodType(List("args"))(_ => List(TypeRepr.of[Array[String]]), _ => TypeRepr.of[Unit]), Flags.Static, Symbol.noSymbol))

        val cls = Symbol.newClass(Symbol.spliceOwner.owner, name, parents = parents.map(_.tpe), decls, selfType = None)
        val mainSym = cls.declaredMethod("main").head

        val mainDef = DefDef(mainSym, _ => Some(Apply(Ref(tree.symbol), Nil)))
        val clsDef = ClassDef(cls, parents, body = List(mainDef))

        List(clsDef, tree)
      case _ =>
        report.error("Annotation only supports `def` without arguments")
        List(tree)
