import annotation.experimental

class Class1:
  import language.experimental.namedTypeArguments // error
  import language.experimental.genericNumberLiterals // error
  import language.experimental.erasedDefinitions // ok: only check at erased definition
  @experimental def f = 1

object Object1:
  import language.experimental.namedTypeArguments // error
  import language.experimental.genericNumberLiterals // error
  import language.experimental.erasedDefinitions // ok: only check at erased definition
  @experimental def f = 1

def fun1 =
  import language.experimental.namedTypeArguments // error
  import language.experimental.genericNumberLiterals // error
  import language.experimental.erasedDefinitions // ok: only check at erased definition
  @experimental def f = 1

val value1 =
  import language.experimental.namedTypeArguments // error
  import language.experimental.genericNumberLiterals // error
  import language.experimental.erasedDefinitions // ok: only check at erased definition
  @experimental def f = 1
