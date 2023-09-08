import caps.unsafe.*
import annotation.unchecked.uncheckedCaptures

object Test:
  type ErrorHandler = (Int, String) => Unit

  @uncheckedCaptures
  var defaultIncompleteHandler: ErrorHandler = ???
  @uncheckedCaptures
  var incompleteHandler: ErrorHandler = defaultIncompleteHandler
  val x = incompleteHandler.unsafeUnbox
  val _ : ErrorHandler = x
  val _ = x(1, "a")

  def defaultIncompleteHandler1(): ErrorHandler = ???
  val defaultIncompleteHandler2: ErrorHandler = ???
  @uncheckedCaptures
  var incompleteHandler1: ErrorHandler = defaultIncompleteHandler1()
  @uncheckedCaptures
  var incompleteHandler2: ErrorHandler = defaultIncompleteHandler2
  @uncheckedCaptures
  private var incompleteHandler7 = defaultIncompleteHandler1()
  @uncheckedCaptures
  private var incompleteHandler8 = defaultIncompleteHandler2

  incompleteHandler1 = defaultIncompleteHandler2
  incompleteHandler1 = defaultIncompleteHandler2
  val saved = incompleteHandler1


