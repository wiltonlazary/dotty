//> using options -Xfatal-warnings -deprecation -feature

package asts

enum Ast[-T >: Null]:
  case DefDef()

trait AstImpl[T >: Null]:
  type Ast = asts.Ast[T]
  type DefDef = Ast.DefDef[T]
end AstImpl

object untpd extends AstImpl[Null]:

  def DefDef(ast: Ast): DefDef = ast match
    case ast: DefDef => ast

end untpd
