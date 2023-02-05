import annotation.experimental

package foo {
  import language.experimental.namedTypeArguments // error
  import language.experimental.genericNumberLiterals // error
  import language.experimental.erasedDefinitions // ok: only check at erased definition

  package bar {
    def foo = 1
  }
}

package foo2 {
  // ok: all definitions are top-level @experimental
  import language.experimental.namedTypeArguments
  import language.experimental.genericNumberLiterals
  import language.experimental.erasedDefinitions

  package bar {
    @experimental def foo = 1
  }
}
