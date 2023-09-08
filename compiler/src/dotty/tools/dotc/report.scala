package dotty.tools.dotc

import reporting._
import Diagnostic._
import util.{SourcePosition, NoSourcePosition, SrcPos}
import core._
import Contexts._, Flags.*, Symbols._, Decorators._
import config.SourceVersion
import ast._
import config.Feature.sourceVersion
import java.lang.System.currentTimeMillis

object report:

  /** For sending messages that are printed only if -verbose is set */
  def inform(msg: => String, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    if ctx.settings.verbose.value then echo(msg, pos)

  def echo(msg: => String, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    ctx.reporter.report(new Info(msg.toMessage, pos.sourcePos))

  private def issueWarning(warning: Warning)(using Context): Unit =
    ctx.reporter.report(warning)

  def deprecationWarning(msg: Message, pos: SrcPos)(using Context): Unit =
    issueWarning(new DeprecationWarning(msg, pos.sourcePos))

  def migrationWarning(msg: Message, pos: SrcPos)(using Context): Unit =
    issueWarning(new MigrationWarning(msg, pos.sourcePos))

  def uncheckedWarning(msg: Message, pos: SrcPos)(using Context): Unit =
    issueWarning(new UncheckedWarning(msg, pos.sourcePos))

  def featureWarning(msg: Message, pos: SrcPos)(using Context): Unit =
    issueWarning(new FeatureWarning(msg, pos.sourcePos))

  def featureWarning(feature: String, featureDescription: => String,
      featureUseSite: Symbol, required: Boolean, pos: SrcPos)(using Context): Unit =
    val req = if required then "needs to" else "should"
    val fqname = s"scala.language.$feature"

    val explain =
      if ctx.reporter.isReportedFeatureUseSite(featureUseSite) then ""
      else
        ctx.reporter.reportNewFeatureUseSite(featureUseSite)
        s"""
           |See the Scala docs for value $fqname for a discussion
           |why the feature $req be explicitly enabled.""".stripMargin

    def msg = em"""$featureDescription $req be enabled
                  |by adding the import clause 'import $fqname'
                  |or by setting the compiler option -language:$feature.$explain"""
    if required then error(msg, pos)
    else issueWarning(new FeatureWarning(msg, pos.sourcePos))
  end featureWarning

  def warning(msg: Message, pos: SrcPos)(using Context): Unit =
    issueWarning(new Warning(msg, addInlineds(pos)))

  def warning(msg: Message)(using Context): Unit =
    warning(msg, NoSourcePosition)

  def warning(msg: => String, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    warning(msg.toMessage, pos)

  def error(msg: Message, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    val fullPos = addInlineds(pos)
    ctx.reporter.report(new Error(msg, fullPos))
    if ctx.settings.YdebugError.value then Thread.dumpStack()

  def error(msg: => String, pos: SrcPos)(using Context): Unit =
    error(msg.toMessage, pos)

  def error(msg: => String)(using Context): Unit =
    error(msg, NoSourcePosition)

  def error(ex: TypeError, pos: SrcPos)(using Context): Unit =
    val fullPos = addInlineds(pos)
    ctx.reporter.report(new StickyError(ex.toMessage, fullPos))
    if ctx.settings.YdebugError.value then Thread.dumpStack()
    if ctx.settings.YdebugTypeError.value then ex.printStackTrace()

  def errorOrMigrationWarning(msg: Message, pos: SrcPos, from: SourceVersion)(using Context): Unit =
    if sourceVersion.isAtLeast(from) then
      if sourceVersion.isMigrating && sourceVersion.ordinal <= from.ordinal then
        if ctx.settings.rewrite.value.isEmpty then migrationWarning(msg, pos)
      else error(msg, pos)

  def gradualErrorOrMigrationWarning(msg: Message, pos: SrcPos, warnFrom: SourceVersion, errorFrom: SourceVersion)(using Context): Unit =
    if sourceVersion.isAtLeast(errorFrom) then errorOrMigrationWarning(msg, pos, errorFrom)
    else if sourceVersion.isAtLeast(warnFrom) then warning(msg, pos)

  def restrictionError(msg: Message, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    error(msg.mapMsg("Implementation restriction: " + _), pos)

  def incompleteInputError(msg: Message, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    ctx.reporter.incomplete(new Error(msg, pos.sourcePos))

  /** Log msg if settings.log contains the current phase.
   *  See [[config.CompilerCommand#explainAdvanced]] for the exact meaning of
   *  "contains" here.
   */
  def log(msg: => String, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    if (ctx.settings.Ylog.value.containsPhase(ctx.phase))
      echo(s"[log ${ctx.phase}] $msg", pos)

  def debuglog(msg: => String)(using Context): Unit =
    if (ctx.debug) log(msg)

  def informTime(msg: => String, start: Long)(using Context): Unit = {
    def elapsed = s" in ${currentTimeMillis - start}ms"
    informProgress(msg + elapsed)
  }

  def informProgress(msg: => String)(using Context): Unit =
    inform("[" + msg + "]")

  def logWith[T](msg: => String)(value: T)(using Context): T = {
    log(msg + " " + value)
    value
  }

  def debugwarn(msg: => String, pos: SrcPos = NoSourcePosition)(using Context): Unit =
    if (ctx.settings.Ydebug.value) warning(msg, pos)

  private def addInlineds(pos: SrcPos)(using Context): SourcePosition =
    def recur(pos: SourcePosition, inlineds: List[Trees.Tree[?]]): SourcePosition = inlineds match
      case inlined :: inlineds1 => pos.withOuter(recur(inlined.sourcePos, inlineds1))
      case Nil => pos
    recur(pos.sourcePos, tpd.enclosingInlineds)

  private object messageRendering extends MessageRendering

  // Should only be called from Run#enrichErrorMessage.
  def enrichErrorMessage(errorMessage: String)(using Context): String = try {
    def formatExplain(pairs: List[(String, Any)]) = pairs.map((k, v) => f"$k%20s: $v").mkString("\n")

    val settings = ctx.settings.userSetSettings(ctx.settingsState).sortBy(_.name)
    val tree     = ctx.tree
    val sym      = tree.symbol
    val pos      = tree.sourcePos
    val path     = pos.source.path
    val site     = ctx.outersIterator.map(_.owner).filter(sym => !sym.exists || sym.isClass || sym.is(Method)).next()

    import untpd.*
    extension (tree: Tree) def summaryString: String = tree match
      case Literal(const)     => s"Literal($const)"
      case Ident(name)        => s"Ident(${name.decode})"
      case Select(qual, name) => s"Select(${qual.summaryString}, ${name.decode})"
      case tree: NameTree     => (if tree.isType then "type " else "") + tree.name.decode
      case tree               => s"${tree.className}${if tree.symbol.exists then s"(${tree.symbol})" else ""}"

    val info1 = formatExplain(List(
      "while compiling"    -> ctx.compilationUnit,
      "during phase"       -> ctx.phase.megaPhase,
      "mode"               -> ctx.mode,
      "library version"    -> scala.util.Properties.versionString,
      "compiler version"   -> dotty.tools.dotc.config.Properties.versionString,
      "settings"           -> settings.map(s => if s.value == "" then s"${s.name} \"\"" else s"${s.name} ${s.value}").mkString(" "),
    ))
    val symbolInfos = if sym eq NoSymbol then List("symbol" -> sym) else List(
      "symbol"             -> sym.showLocated,
      "symbol definition"  -> s"${sym.showDcl} (a ${sym.className})",
      "symbol package"     -> sym.enclosingPackageClass.fullName,
      "symbol owners"      -> sym.showExtendedLocation,
    )
    val info2 = formatExplain(List(
      "tree"               -> tree.summaryString,
      "tree position"      -> (if pos.exists then s"$path:${pos.line + 1}:${pos.column}" else s"$path:<unknown>"),
      "tree type"          -> tree.typeOpt.show,
    ) ::: symbolInfos ::: List(
      "call site"          -> s"${site.showLocated} in ${site.enclosingPackageClass}"
    ))
    val context_s = try
      s"""  == Source file context for tree position ==
         |
         |${messageRendering.messageAndPos(Diagnostic.Error("", pos))}""".stripMargin
    catch case _: Exception => "<Cannot read source file>"
    s"""
       |  $errorMessage
       |
       |  An unhandled exception was thrown in the compiler.
       |  Please file a crash report here:
       |  https://github.com/lampepfl/dotty/issues/new/choose
       |
       |$info1
       |
       |$info2
       |
       |$context_s""".stripMargin
  } catch case _: Throwable => errorMessage // don't introduce new errors trying to report errors, so swallow exceptions
end report
