package dotty.tools.dotc.transform

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.ast.tpd.{Inlined, TreeTraverser}
import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.ast.untpd.ImportSelector
import dotty.tools.dotc.config.ScalaSettings
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Decorators.{em, i}
import dotty.tools.dotc.core.Flags.*
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.core.StdNames
import dotty.tools.dotc.report
import dotty.tools.dotc.reporting.Message
import dotty.tools.dotc.typer.ImportInfo
import dotty.tools.dotc.util.{Property, SrcPos}
import dotty.tools.dotc.core.Mode
import dotty.tools.dotc.core.Types.{AnnotatedType, ConstantType, NoType, TermRef, Type, TypeTraverser}
import dotty.tools.dotc.core.Flags.flagsString
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Names.Name
import dotty.tools.dotc.transform.MegaPhase.MiniPhase
import dotty.tools.dotc.core.Annotations
import dotty.tools.dotc.core.Definitions
import dotty.tools.dotc.core.NameKinds.WildcardParamName
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.StdNames.nme


/**
 * A compiler phase that checks for unused imports or definitions
 *
 * Basically, it gathers definition/imports and their usage. If a
 * definition/imports does not have any usage, then it is reported.
 */
class CheckUnused extends MiniPhase:
  import CheckUnused.UnusedData

  /**
   * The key used to retrieve the "unused entity" analysis metadata,
   * from the compilation `Context`
   */
  private val _key = Property.Key[UnusedData]

  private def unusedDataApply[U](f: UnusedData => U)(using Context): Context =
    ctx.property(_key).foreach(f)
    ctx
  private def getUnusedData(using Context): Option[UnusedData] =
    ctx.property(_key)

  override def phaseName: String = CheckUnused.phaseName

  override def description: String = CheckUnused.description

  override def isRunnable(using Context): Boolean =
    ctx.settings.Wunused.value.nonEmpty &&
    !ctx.isJava

  // ========== SETUP ============

  override def prepareForUnit(tree: tpd.Tree)(using Context): Context =
    val data = UnusedData()
    val fresh = ctx.fresh.setProperty(_key, data)
    fresh

  // ========== END + REPORTING ==========

  override def transformUnit(tree: tpd.Tree)(using Context): tpd.Tree =
    unusedDataApply(ud => reportUnused(ud.getUnused))
    tree

  // ========== MiniPhase Prepare ==========
  override def prepareForOther(tree: tpd.Tree)(using Context): Context =
    // A standard tree traverser covers cases not handled by the Mega/MiniPhase
    traverser.traverse(tree)
    ctx

  override def prepareForInlined(tree: tpd.Inlined)(using Context): Context =
    traverser.traverse(tree.call)
    ctx

  override def prepareForIdent(tree: tpd.Ident)(using Context): Context =
    if tree.symbol.exists then
      val prefixes = LazyList.iterate(tree.typeOpt.normalizedPrefix)(_.normalizedPrefix).takeWhile(_ != NoType)
        .take(10) // Failsafe for the odd case if there was an infinite cycle
      for prefix <- prefixes do
        unusedDataApply(_.registerUsed(prefix.classSymbol, None))
      unusedDataApply(_.registerUsed(tree.symbol, Some(tree.name)))
    else if tree.hasType then
      unusedDataApply(_.registerUsed(tree.tpe.classSymbol, Some(tree.name)))
    else
      ctx

  override def prepareForSelect(tree: tpd.Select)(using Context): Context =
    unusedDataApply(_.registerUsed(tree.symbol, Some(tree.name)))

  override def prepareForBlock(tree: tpd.Block)(using Context): Context =
    pushInBlockTemplatePackageDef(tree)

  override def prepareForTemplate(tree: tpd.Template)(using Context): Context =
    pushInBlockTemplatePackageDef(tree)

  override def prepareForPackageDef(tree: tpd.PackageDef)(using Context): Context =
    pushInBlockTemplatePackageDef(tree)

  override def prepareForValDef(tree: tpd.ValDef)(using Context): Context =
    unusedDataApply{ud =>
      // do not register the ValDef generated for `object`
      traverseAnnotations(tree.symbol)
      if !tree.symbol.is(Module) then
        ud.registerDef(tree)
      if tree.name.mangledString.startsWith(nme.derived.mangledString + "$")
          && tree.typeOpt != NoType then
        ud.registerUsed(tree.typeOpt.typeSymbol, None, true)
      ud.addIgnoredUsage(tree.symbol)
    }

  override def prepareForDefDef(tree: tpd.DefDef)(using Context): Context =
    unusedDataApply{ ud =>
      import ud.registerTrivial
      tree.registerTrivial
      traverseAnnotations(tree.symbol)
      ud.registerDef(tree)
      ud.addIgnoredUsage(tree.symbol)
    }

  override def prepareForTypeDef(tree: tpd.TypeDef)(using Context): Context =
    unusedDataApply{ ud =>
      if !tree.symbol.is(Param) then // Ignore type parameter (as Scala 2)
        traverseAnnotations(tree.symbol)
        ud.registerDef(tree)
        ud.addIgnoredUsage(tree.symbol)
    }

  override def prepareForBind(tree: tpd.Bind)(using Context): Context =
    traverseAnnotations(tree.symbol)
    unusedDataApply(_.registerPatVar(tree))

  override def prepareForTypeTree(tree: tpd.TypeTree)(using Context): Context =
    if !tree.isInstanceOf[tpd.InferredTypeTree] then typeTraverser(unusedDataApply).traverse(tree.tpe)
    ctx

  // ========== MiniPhase Transform ==========

  override def transformBlock(tree: tpd.Block)(using Context): tpd.Tree =
    popOutBlockTemplatePackageDef()
    tree

  override def transformTemplate(tree: tpd.Template)(using Context): tpd.Tree =
    popOutBlockTemplatePackageDef()
    tree

  override def transformPackageDef(tree: tpd.PackageDef)(using Context): tpd.Tree =
    popOutBlockTemplatePackageDef()
    tree

  override def transformValDef(tree: tpd.ValDef)(using Context): tpd.Tree =
    unusedDataApply(_.removeIgnoredUsage(tree.symbol))
    tree

  override def transformDefDef(tree: tpd.DefDef)(using Context): tpd.Tree =
    unusedDataApply(_.removeIgnoredUsage(tree.symbol))
    tree

  override def transformTypeDef(tree: tpd.TypeDef)(using Context): tpd.Tree =
    unusedDataApply(_.removeIgnoredUsage(tree.symbol))
    tree

  // ---------- MiniPhase HELPERS -----------

  private def pushInBlockTemplatePackageDef(tree: tpd.Block | tpd.Template | tpd.PackageDef)(using Context): Context =
    unusedDataApply { ud =>
      ud.pushScope(UnusedData.ScopeType.fromTree(tree))
    }
    ctx

  private def popOutBlockTemplatePackageDef()(using Context): Context =
    unusedDataApply { ud =>
      ud.popScope()
    }
    ctx

  private def newCtx(tree: tpd.Tree)(using Context) =
    if tree.symbol.exists then ctx.withOwner(tree.symbol) else ctx

  /**
   * This traverse is the **main** component of this phase
   *
   * It traverse the tree the tree and gather the data in the
   * corresponding context property
   */
  private def traverser = new TreeTraverser:
    import tpd._
    import UnusedData.ScopeType

    /* Register every imports, definition and usage */
    override def traverse(tree: tpd.Tree)(using Context): Unit =
      val newCtx = if tree.symbol.exists then ctx.withOwner(tree.symbol) else ctx
      tree match
        case imp:tpd.Import =>
          unusedDataApply(_.registerImport(imp))
          traverseChildren(tree)(using newCtx)
        case ident: Ident =>
          prepareForIdent(ident)
          traverseChildren(tree)(using newCtx)
        case sel: Select =>
          prepareForSelect(sel)
          traverseChildren(tree)(using newCtx)
        case _: (tpd.Block | tpd.Template | tpd.PackageDef) =>
          //! DIFFERS FROM MINIPHASE
          unusedDataApply { ud =>
            ud.inNewScope(ScopeType.fromTree(tree))(traverseChildren(tree)(using newCtx))
          }
        case t:tpd.ValDef =>
          prepareForValDef(t)
          traverseChildren(tree)(using newCtx)
          transformValDef(t)
        case t:tpd.DefDef =>
          prepareForDefDef(t)
          traverseChildren(tree)(using newCtx)
          transformDefDef(t)
        case t:tpd.TypeDef =>
          prepareForTypeDef(t)
          traverseChildren(tree)(using newCtx)
          transformTypeDef(t)
        case t: tpd.Bind =>
          prepareForBind(t)
          traverseChildren(tree)(using newCtx)
        case _: tpd.InferredTypeTree =>
        case t@tpd.TypeTree() =>
          //! DIFFERS FROM MINIPHASE
          typeTraverser(unusedDataApply).traverse(t.tpe)
          traverseChildren(tree)(using newCtx)
        case _ =>
          //! DIFFERS FROM MINIPHASE
          traverseChildren(tree)(using newCtx)
    end traverse
  end traverser

  /** This is a type traverser which catch some special Types not traversed by the term traverser above */
  private def typeTraverser(dt: (UnusedData => Any) => Unit)(using Context) = new TypeTraverser:
    override def traverse(tp: Type): Unit =
      if tp.typeSymbol.exists then dt(_.registerUsed(tp.typeSymbol, Some(tp.typeSymbol.name)))
      tp match
        case AnnotatedType(_, annot) =>
          dt(_.registerUsed(annot.symbol, None))
          traverseChildren(tp)
        case _ =>
          traverseChildren(tp)

  /** This traverse the annotations of the symbol */
  private def traverseAnnotations(sym: Symbol)(using Context): Unit =
    sym.denot.annotations.foreach(annot => traverser.traverse(annot.tree))

  /** Do the actual reporting given the result of the anaylsis */
  private def reportUnused(res: UnusedData.UnusedResult)(using Context): Unit =
    import CheckUnused.WarnTypes
    res.warnings.foreach { s =>
      s match
        case (t, WarnTypes.Imports) =>
          report.warning(s"unused import", t)
        case (t, WarnTypes.LocalDefs) =>
          report.warning(s"unused local definition", t)
        case (t, WarnTypes.ExplicitParams) =>
          report.warning(s"unused explicit parameter", t)
        case (t, WarnTypes.ImplicitParams) =>
          report.warning(s"unused implicit parameter", t)
        case (t, WarnTypes.PrivateMembers) =>
          report.warning(s"unused private member", t)
        case (t, WarnTypes.PatVars) =>
          report.warning(s"unused pattern variable", t)
    }

end CheckUnused

object CheckUnused:
  val phaseName: String = "checkUnused"
  val description: String = "check for unused elements"

  private enum WarnTypes:
    case Imports
    case LocalDefs
    case ExplicitParams
    case ImplicitParams
    case PrivateMembers
    case PatVars

  /**
   * A stateful class gathering the infos on :
   * - imports
   * - definitions
   * - usage
   */
  private class UnusedData:
    import dotty.tools.dotc.transform.CheckUnused.UnusedData.UnusedResult
    import collection.mutable.{Set => MutSet, Map => MutMap, Stack => MutStack}
    import UnusedData.ScopeType

    /** The current scope during the tree traversal */
    var currScopeType: MutStack[ScopeType] = MutStack(ScopeType.Other)

    /* IMPORTS */
    private val impInScope = MutStack(MutSet[tpd.Import]())
    /**
     * We store the symbol along with their accessibility without import.
     * Accessibility to their definition in outer context/scope
     *
     * See the `isAccessibleAsIdent` extension method below in the file
     */
    private val usedInScope = MutStack(MutSet[(Symbol,Boolean, Option[Name], Boolean)]())
    private val usedInPosition = MutSet[(SrcPos, Name)]()
    /* unused import collected during traversal */
    private val unusedImport = MutSet[ImportSelector]()

    /* LOCAL DEF OR VAL / Private Def or Val / Pattern variables */
    private val localDefInScope = MutSet[tpd.MemberDef]()
    private val privateDefInScope = MutSet[tpd.MemberDef]()
    private val explicitParamInScope = MutSet[tpd.MemberDef]()
    private val implicitParamInScope = MutSet[tpd.MemberDef]()
    private val patVarsInScope = MutSet[tpd.Bind]()

    /* Unused collection collected at the end */
    private val unusedLocalDef = MutSet[tpd.MemberDef]()
    private val unusedPrivateDef = MutSet[tpd.MemberDef]()
    private val unusedExplicitParams = MutSet[tpd.MemberDef]()
    private val unusedImplicitParams = MutSet[tpd.MemberDef]()
    private val unusedPatVars = MutSet[tpd.Bind]()

    /** All used symbols */
    private val usedDef = MutSet[Symbol]()
    /** Do not register as used */
    private val doNotRegister = MutSet[Symbol]()

    /** Trivial definitions, avoid registering params */
    private val trivialDefs = MutSet[Symbol]()

    /**
     * Push a new Scope of the given type, executes the given Unit and
     * pop it back to the original type.
     */
    def inNewScope(newScope: ScopeType)(execInNewScope: => Unit)(using Context): Unit =
      val prev = currScopeType
      pushScope(newScope)
      execInNewScope
      popScope()

    /**
     * Register a found (used) symbol along with its name
     *
     * The optional name will be used to target the right import
     * as the same element can be imported with different renaming
     */
    def registerUsed(sym: Symbol, name: Option[Name], isDerived: Boolean = false)(using Context): Unit =
      if !isConstructorOfSynth(sym) && !doNotRegister(sym) then
        if sym.isConstructor && sym.exists then
          registerUsed(sym.owner, None) // constructor are "implicitly" imported with the class
        else
          usedInScope.top += ((sym, sym.isAccessibleAsIdent, name, isDerived))
          usedInScope.top += ((sym.companionModule, sym.isAccessibleAsIdent, name, isDerived))
          usedInScope.top += ((sym.companionClass, sym.isAccessibleAsIdent, name, isDerived))
          name.map(n => usedInPosition += ((sym.sourcePos, n)))

    /** Register a symbol that should be ignored */
    def addIgnoredUsage(sym: Symbol)(using Context): Unit =
      doNotRegister ++= sym.everySymbol

    /** Remove a symbol that shouldn't be ignored anymore */
    def removeIgnoredUsage(sym: Symbol)(using Context): Unit =
      doNotRegister --= sym.everySymbol


    /** Register an import */
    def registerImport(imp: tpd.Import)(using Context): Unit =
      if !tpd.languageImport(imp.expr).nonEmpty && !imp.isGeneratedByEnum && !isTransparentAndInline(imp) then
        impInScope.top += imp
        unusedImport ++= imp.selectors.filter { s =>
          !shouldSelectorBeReported(imp, s) && !isImportExclusion(s)
        }

    /** Register (or not) some `val` or `def` according to the context, scope and flags */
    def registerDef(memDef: tpd.MemberDef)(using Context): Unit =
      if memDef.isValidMemberDef then
        if memDef.isValidParam then
          if memDef.symbol.isOneOf(GivenOrImplicit) then
            implicitParamInScope += memDef
          else
            explicitParamInScope += memDef
        else if currScopeType.top == ScopeType.Local then
          localDefInScope += memDef
        else if memDef.shouldReportPrivateDef then
          privateDefInScope += memDef

    /** Register pattern variable */
    def registerPatVar(patvar: tpd.Bind)(using Context): Unit =
      if !patvar.symbol.isUnusedAnnot then
        patVarsInScope += patvar

    /** enter a new scope */
    def pushScope(newScopeType: ScopeType): Unit =
      // unused imports :
      currScopeType.push(newScopeType)
      impInScope.push(MutSet())
      usedInScope.push(MutSet())

    /**
     * leave the current scope and do :
     *
     * - If there are imports in this scope check for unused ones
     */
    def popScope()(using Context): Unit =
      // used symbol in this scope
      val used = usedInScope.pop().toSet
      // used imports in this scope
      val imports = impInScope.pop()
      val kept = used.filterNot { t =>
        val (sym, isAccessible, optName, isDerived) = t
        // keep the symbol for outer scope, if it matches **no** import
        // This is the first matching wildcard selector
        var selWildCard: Option[ImportSelector] = None

        val exists = imports.exists { imp =>
          sym.isInImport(imp, isAccessible, optName, isDerived) match
            case None => false
            case optSel@Some(sel) if sel.isWildcard =>
              if selWildCard.isEmpty then selWildCard = optSel
              // We keep wildcard symbol for the end as they have the least precedence
              false
            case Some(sel) =>
              unusedImport -= sel
              true
        }
        if !exists && selWildCard.isDefined then
          unusedImport -= selWildCard.get
          true // a matching import exists so the symbol won't be kept for outer scope
        else
          exists
      }

      // if there's an outer scope
      if usedInScope.nonEmpty then
        // we keep the symbols not referencing an import in this scope
        // as it can be the only reference to an outer import
        usedInScope.top ++= kept
      // register usage in this scope for other warnings at the end of the phase
      usedDef ++= used.map(_._1)
      // retrieve previous scope type
      currScopeType.pop
    end popScope

    /**
     * Leave the scope and return a `List` of unused `ImportSelector`s
     *
     * The given `List` is sorted by line and then column of the position
     */
    def getUnused(using Context): UnusedResult =
      popScope()

      val sortedImp =
        if ctx.settings.WunusedHas.imports || ctx.settings.WunusedHas.strictNoImplicitWarn then
          unusedImport.map(d => d.srcPos -> WarnTypes.Imports).toList
        else
          Nil
      val sortedLocalDefs =
        if ctx.settings.WunusedHas.locals then
          localDefInScope
            .filterNot(d => d.symbol.usedDefContains)
            .filterNot(d => usedInPosition.exists { case (pos, name) => d.span.contains(pos.span) && name == d.symbol.name})
            .filterNot(d => containsSyntheticSuffix(d.symbol))
            .map(d => d.namePos -> WarnTypes.LocalDefs).toList
        else
          Nil
      val sortedExplicitParams =
        if ctx.settings.WunusedHas.explicits then
          explicitParamInScope
            .filterNot(d => d.symbol.usedDefContains)
            .filterNot(d => containsSyntheticSuffix(d.symbol))
            .map(d => d.namePos -> WarnTypes.ExplicitParams).toList
        else
          Nil
      val sortedImplicitParams =
        if ctx.settings.WunusedHas.implicits then
          implicitParamInScope
            .filterNot(d => d.symbol.usedDefContains)
            .filterNot(d => containsSyntheticSuffix(d.symbol))
            .map(d => d.namePos -> WarnTypes.ImplicitParams).toList
        else
          Nil
      val sortedPrivateDefs =
        if ctx.settings.WunusedHas.privates then
          privateDefInScope
            .filterNot(d => d.symbol.usedDefContains)
            .filterNot(d => containsSyntheticSuffix(d.symbol))
            .map(d => d.namePos -> WarnTypes.PrivateMembers).toList
        else
          Nil
      val sortedPatVars =
        if ctx.settings.WunusedHas.patvars then
          patVarsInScope
            .filterNot(d => d.symbol.usedDefContains)
            .filterNot(d => containsSyntheticSuffix(d.symbol))
            .filterNot(d => usedInPosition.exists { case (pos, name) => d.span.contains(pos.span) && name == d.symbol.name})
            .map(d => d.namePos -> WarnTypes.PatVars).toList
        else
          Nil
      val warnings = List(sortedImp, sortedLocalDefs, sortedExplicitParams, sortedImplicitParams, sortedPrivateDefs, sortedPatVars).flatten.sortBy { s =>
        val pos = s._1.sourcePos
        (pos.line, pos.column)
      }
      UnusedResult(warnings, Nil)
    end getUnused
    //============================ HELPERS ====================================


    /**
     * Checks if import selects a def that is transparent and inline
     */
    private def isTransparentAndInline(imp: tpd.Import)(using Context): Boolean =
      imp.selectors.exists { sel =>
        val qual = imp.expr
        val importedMembers = qual.tpe.member(sel.name).alternatives.map(_.symbol)
        importedMembers.exists(s => s.is(Transparent) && s.is(Inline))
      }

    /**
     * Heuristic to detect synthetic suffixes in names of symbols
     */
    private def containsSyntheticSuffix(symbol: Symbol)(using Context): Boolean =
      symbol.name.mangledString.contains("$")

    /**
     * Is the the constructor of synthetic package object
     * Should be ignored as it is always imported/used in package
     * Trigger false negative on used import
     *
     * Without this check example:
     *
     * --- WITH PACKAGE : WRONG ---
     * {{{
     * package a:
     *   val x: Int = 0
     * package b:
     *   import a._ // no warning
     * }}}
     * --- WITH OBJECT : OK ---
     * {{{
     * object a:
     *   val x: Int = 0
     * object b:
     *   import a._ // unused warning
     * }}}
     */
    private def isConstructorOfSynth(sym: Symbol)(using Context): Boolean =
      sym.exists && sym.isConstructor && sym.owner.isPackageObject && sym.owner.is(Synthetic)

    /**
     * This is used to avoid reporting the parameters of the synthetic main method
     * generated by `@main`
     */
    private def isSyntheticMainParam(sym: Symbol)(using Context): Boolean =
      sym.exists && ctx.platform.isMainMethod(sym.owner) && sym.owner.is(Synthetic)

    /**
     * This is used to ignore exclusion imports (i.e. import `qual`.{`member` => _})
     */
    private def isImportExclusion(sel: ImportSelector): Boolean = sel.renamed match
      case untpd.Ident(name) => name == StdNames.nme.WILDCARD
      case _ => false

    /**
     * If -Wunused:strict-no-implicit-warn import and this import selector could potentially import implicit.
     * return true
     */
    private def shouldSelectorBeReported(imp: tpd.Import, sel: ImportSelector)(using Context): Boolean =
      if ctx.settings.WunusedHas.strictNoImplicitWarn then
        sel.isWildcard ||
        imp.expr.tpe.member(sel.name.toTermName).alternatives.exists(_.symbol.isOneOf(GivenOrImplicit)) ||
        imp.expr.tpe.member(sel.name.toTypeName).alternatives.exists(_.symbol.isOneOf(GivenOrImplicit))
      else
        false

    extension (sym: Symbol)
      /** is accessible without import in current context */
      private def isAccessibleAsIdent(using Context): Boolean =
        sym.exists &&
          ctx.outersIterator.exists{ c =>
            c.owner == sym.owner
            || sym.owner.isClass && c.owner.isClass
                && c.owner.thisType.baseClasses.contains(sym.owner)
                && c.owner.thisType.member(sym.name).alternatives.contains(sym)
          }

      /** Given an import and accessibility, return an option of selector that match import<->symbol */
      private def isInImport(imp: tpd.Import, isAccessible: Boolean, symName: Option[Name], isDerived: Boolean)(using Context): Option[ImportSelector] =
        val tpd.Import(qual, sels) = imp
        val dealiasedSym = dealias(sym)
        val simpleSelections = qual.tpe.member(sym.name).alternatives
        val typeSelections = sels.flatMap(n => qual.tpe.member(n.name.toTypeName).alternatives)
        val termSelections = sels.flatMap(n => qual.tpe.member(n.name.toTermName).alternatives)
        val selectionsToDealias = typeSelections ::: termSelections
        val qualHasSymbol = simpleSelections.map(_.symbol).contains(sym) || (simpleSelections ::: selectionsToDealias).map(_.symbol).map(dealias).contains(dealiasedSym)
        def selector = sels.find(sel => (sel.name.toTermName == sym.name || sel.name.toTypeName == sym.name) && symName.map(n => n.toTermName == sel.rename).getOrElse(true))
        def dealiasedSelector = if(isDerived) sels.flatMap(sel => selectionsToDealias.map(m => (sel, m.symbol))).collect {
          case (sel, sym) if dealias(sym) == dealiasedSym => sel
        }.headOption else None
        def wildcard = sels.find(sel => sel.isWildcard && ((sym.is(Given) == sel.isGiven) || sym.is(Implicit)))
        if qualHasSymbol && !isAccessible && sym.exists then
          selector.orElse(dealiasedSelector).orElse(wildcard) // selector with name or wildcard (or given)
        else
          None


      private def dealias(symbol: Symbol)(using Context): Symbol =
        if(symbol.isType && symbol.asType.denot.isAliasType) then
          symbol.asType.typeRef.dealias.typeSymbol
        else symbol
      /** Annotated with @unused */
      private def isUnusedAnnot(using Context): Boolean =
        sym.annotations.exists(a => a.symbol == ctx.definitions.UnusedAnnot)

      private def shouldNotReportParamOwner(using Context): Boolean =
        if sym.exists then
          val owner = sym.owner
          trivialDefs(owner) || // is a trivial def
          owner.isPrimaryConstructor ||
          owner.annotations.exists ( // @depreacated
            _.symbol == ctx.definitions.DeprecatedAnnot
          ) ||
          owner.isAllOf(Synthetic | PrivateLocal) ||
          owner.is(Accessor) ||
          owner.isOverriden
        else
          false

      private def usedDefContains(using Context): Boolean =
        sym.everySymbol.exists(usedDef.apply)

      private def everySymbol(using Context): List[Symbol] =
        List(sym, sym.companionClass, sym.companionModule, sym.moduleClass).filter(_.exists)

      /** A function is overriden. Either has `override flags` or parent has a matching member (type and name) */
      private def isOverriden(using Context): Boolean =
        sym.is(Flags.Override) ||
        (if sym.exists then sym.owner.thisType.parents.exists(p => sym.matchingMember(p).exists) else false)

    end extension

    extension (defdef: tpd.DefDef)
      // so trivial that it never consumes params
      private def isTrivial(using Context): Boolean =
        val rhs = defdef.rhs
        rhs.symbol == ctx.definitions.Predef_undefined ||
        rhs.tpe =:= ctx.definitions.NothingType ||
        defdef.symbol.is(Deferred) ||
        (rhs match {
          case _: tpd.Literal => true
          case _ => rhs.tpe match
            case ConstantType(_) => true
            case tp: TermRef =>
              // Detect Scala 2 SingleType
              tp.underlying.classSymbol.is(Flags.Module)
            case _ =>
              false
        })
      def registerTrivial(using Context): Unit =
        if defdef.isTrivial then
          trivialDefs += defdef.symbol

    extension (memDef: tpd.MemberDef)
      private def isValidMemberDef(using Context): Boolean =
        !memDef.symbol.isUnusedAnnot && !memDef.symbol.isAllOf(Flags.AccessorCreationFlags) && !memDef.name.isWildcard && !memDef.symbol.owner.is(Extension)

      private def isValidParam(using Context): Boolean =
        val sym = memDef.symbol
        (sym.is(Param) || sym.isAllOf(PrivateParamAccessor | Local, butNot = CaseAccessor)) &&
        !isSyntheticMainParam(sym) &&
        !sym.shouldNotReportParamOwner


      private def shouldReportPrivateDef(using Context): Boolean =
        currScopeType.top == ScopeType.Template && !memDef.symbol.isConstructor && memDef.symbol.is(Private, butNot = SelfName | Synthetic | CaseAccessor)

    extension (imp: tpd.Import)
      /** Enum generate an import for its cases (but outside them), which should be ignored */
      def isGeneratedByEnum(using Context): Boolean =
        imp.symbol.exists && imp.symbol.owner.is(Flags.Enum, butNot = Flags.Case)

    extension (thisName: Name)
      private def isWildcard: Boolean =
        thisName == StdNames.nme.WILDCARD || thisName.is(WildcardParamName)

  end UnusedData

  private object UnusedData:
      enum ScopeType:
        case Local
        case Template
        case Other

      object ScopeType:
        /** return the scope corresponding to the enclosing scope of the given tree */
        def fromTree(tree: tpd.Tree): ScopeType = tree match
          case _:tpd.Template => Template
          case _:tpd.Block => Local
          case _ => Other

      /** A container for the results of the used elements analysis */
      case class UnusedResult(warnings: List[(dotty.tools.dotc.util.SrcPos, WarnTypes)], usedImports: List[(tpd.Import, untpd.ImportSelector)])
end CheckUnused

