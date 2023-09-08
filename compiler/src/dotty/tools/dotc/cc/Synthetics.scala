package dotty.tools
package dotc
package cc

import core.*
import Symbols.*, SymDenotations.*, Contexts.*, Flags.*, Types.*, Decorators.*
import StdNames.nme
import Names.Name
import NameKinds.DefaultGetterName
import Phases.checkCapturesPhase
import config.Printers.capt

/** Classification and transformation methods for function methods and
 *  synthetic case class methods that need to be treated specially.
 *  In particular, compute capturing types for some of these methods which
 *  have inferred (result-)types that need to be established under separate
 *  compilation.
 */
object Synthetics:
  private def isSyntheticCopyMethod(sym: SymDenotation)(using Context) =
    sym.name == nme.copy && sym.is(Synthetic) && sym.owner.isClass && sym.owner.is(Case)

  private def isSyntheticCompanionMethod(sym: SymDenotation, names: Name*)(using Context): Boolean =
     names.contains(sym.name) && sym.is(Synthetic) && sym.owner.is(Module) && sym.owner.companionClass.is(Case)

  private def isSyntheticCopyDefaultGetterMethod(sym: SymDenotation)(using Context) = sym.name match
    case DefaultGetterName(nme.copy, _) => sym.is(Synthetic) && sym.owner.isClass && sym.owner.is(Case)
    case _ => false

  private val functionCombinatorNames = Set[Name](
    nme.andThen, nme.compose, nme.curried, nme.tupled)

  /** Is `sym` a synthetic apply, copy, or copy default getter method?
   *  The types of these symbols are transformed in a special way without
   *  looking at the definitions's RHS
   */
  def needsTransform(symd: SymDenotation)(using Context): Boolean =
    isSyntheticCopyMethod(symd)
    || isSyntheticCompanionMethod(symd, nme.apply, nme.unapply)
    || isSyntheticCopyDefaultGetterMethod(symd)
    || (symd.symbol eq defn.Object_eq)
    || (symd.symbol eq defn.Object_ne)
    || defn.isFunctionClass(symd.owner) && functionCombinatorNames.contains(symd.name)

  /** Method is excluded from regular capture checking.
   *  Excluded are synthetic class members
   *   - that override a synthesized case class symbol, or
   *   - the fromProduct method, or
   *   - members transformed specially as indicated by `needsTransform`.
   */
  def isExcluded(sym: Symbol)(using Context): Boolean =
    sym.is(Synthetic)
    && sym.owner.isClass
    && ( defn.caseClassSynthesized.exists(
             ccsym => sym.overriddenSymbol(ccsym.owner.asClass) == ccsym)
        || isSyntheticCompanionMethod(sym, nme.fromProduct)
        || needsTransform(sym))

  /** Transform the type of a method either to its type under capture checking
   *  or back to its previous type.
   *  @param  sym  The method to transform @pre needsTransform(sym) must hold.
   *  @param  toCC Whether to transform the type to capture checking or back.
   */
  def transform(sym: SymDenotation, toCC: Boolean)(using Context): SymDenotation =

    /** Add capture dependencies to the type of the `apply` or `copy` method of a case class.
     *  An apply method in a case class like this:
     *    case class CC(a: A^{d}, b: B, c: C^{cap})
     *  would get type
     *    def apply(a': A^{d}, b: B, c': C^{cap}): CC^{a', c'} { val a = A^{a'}, val c = C^{c'} }
     *  where `'` is used to indicate the difference between parameter symbol and refinement name.
     *  Analogous for the copy method.
     */
    def addCaptureDeps(info: Type): Type = info match
      case info: MethodType =>
        val trackedParams = info.paramRefs.filter(atPhase(checkCapturesPhase)(_.isTracked))
        def augmentResult(tp: Type): Type = tp match
          case tp: MethodOrPoly =>
            tp.derivedLambdaType(resType = augmentResult(tp.resType))
          case _ =>
            val refined = trackedParams.foldLeft(tp) { (parent, pref) =>
              RefinedType(parent, pref.paramName,
                CapturingType(
                  atPhase(ctx.phase.next)(pref.underlying.stripCapturing),
                  CaptureSet(pref)))
            }
            CapturingType(refined, CaptureSet(trackedParams*))
        if trackedParams.isEmpty then info
        else augmentResult(info).showing(i"augment apply/copy type $info to $result", capt)
      case info: PolyType =>
        info.derivedLambdaType(resType = addCaptureDeps(info.resType))
      case _ =>
        info

    /** Drop capture dependencies from the type of `apply` or `copy` method of a case class */
    def dropCaptureDeps(tp: Type): Type = tp match
      case tp: MethodOrPoly =>
        tp.derivedLambdaType(resType = dropCaptureDeps(tp.resType))
      case CapturingType(parent, _) =>
        dropCaptureDeps(parent)
      case RefinedType(parent, _, _) =>
        dropCaptureDeps(parent)
      case _ =>
        tp

    /** Add capture information to the type of the default getter of a case class copy method
     *  if toCC = true, or remove the added info again if toCC = false.
     */
    def transformDefaultGetterCaptures(info: Type, owner: Symbol, idx: Int)(using Context): Type = info match
      case info: MethodOrPoly =>
        info.derivedLambdaType(resType = transformDefaultGetterCaptures(info.resType, owner, idx))
      case info: ExprType =>
        info.derivedExprType(transformDefaultGetterCaptures(info.resType, owner, idx))
      case EventuallyCapturingType(parent, _) =>
        if toCC then transformDefaultGetterCaptures(parent, owner, idx)
        else parent
      case info @ AnnotatedType(parent, annot) =>
        info.derivedAnnotatedType(transformDefaultGetterCaptures(parent, owner, idx), annot)
      case _ if toCC && idx < owner.asClass.paramGetters.length =>
        val param = owner.asClass.paramGetters(idx)
        val pinfo = param.info
        atPhase(ctx.phase.next) {
          if pinfo.captureSet.isAlwaysEmpty then info
          else CapturingType(pinfo.stripCapturing, CaptureSet(param.termRef))
        }
      case _ =>
        info

    /** Augment an unapply of type `(x: C): D` to `(x: C^{cap}): D^{x}` if toCC is true,
     *  or remove the added capture sets again if toCC = false.
     */
    def transformUnapplyCaptures(info: Type)(using Context): Type = info match
      case info: MethodType =>
        if toCC then
          val paramInfo :: Nil = info.paramInfos: @unchecked
          val newParamInfo = CapturingType(paramInfo, CaptureSet.universal)
          val trackedParam = info.paramRefs.head
          def newResult(tp: Type): Type = tp match
            case tp: MethodOrPoly =>
              tp.derivedLambdaType(resType = newResult(tp.resType))
            case _ =>
              CapturingType(tp, CaptureSet(trackedParam))
          info.derivedLambdaType(paramInfos = newParamInfo :: Nil, resType = newResult(info.resType))
            .showing(i"augment unapply type $info to $result", capt)
        else info.paramInfos match
          case CapturingType(oldParamInfo, _) :: Nil =>
            def oldResult(tp: Type): Type = tp match
              case tp: MethodOrPoly =>
                tp.derivedLambdaType(resType = oldResult(tp.resType))
              case CapturingType(tp, _) =>
                tp
            info.derivedLambdaType(paramInfos = oldParamInfo :: Nil, resType = oldResult(info.resType))
          case _ =>
            info
      case info: PolyType =>
        info.derivedLambdaType(resType = transformUnapplyCaptures(info.resType))

    def transformComposeCaptures(symd: SymDenotation) =
      val (pt: PolyType) = symd.info: @unchecked
      val (mt: MethodType) = pt.resType: @unchecked
      val (enclThis: ThisType) = symd.owner.thisType: @unchecked
      val mt1 =
        if toCC then
          MethodType(mt.paramNames)(
            mt1 => mt.paramInfos.map(_.capturing(CaptureSet.universal)),
            mt1 => CapturingType(mt.resType, CaptureSet(enclThis, mt1.paramRefs.head)))
        else
          MethodType(mt.paramNames)(
            mt1 => mt.paramInfos.map(_.stripCapturing),
            mt1 => mt.resType.stripCapturing)
      pt.derivedLambdaType(resType = mt1)

    def transformCurriedTupledCaptures(symd: SymDenotation) =
      val (et: ExprType) = symd.info: @unchecked
      val (enclThis: ThisType) = symd.owner.thisType: @unchecked
      def mapFinalResult(tp: Type, f: Type => Type): Type =
        val defn.FunctionOf(args, res, isContextual) = tp: @unchecked
        if defn.isFunctionNType(res) then
          defn.FunctionOf(args, mapFinalResult(res, f), isContextual)
        else
          f(tp)
      val resType1 =
        if toCC then
          mapFinalResult(et.resType, CapturingType(_, CaptureSet(enclThis)))
        else
          et.resType.stripCapturing
      ExprType(resType1)

    def transformCompareCaptures =
      if toCC then
        MethodType(defn.ObjectType.capturing(CaptureSet.universal) :: Nil, defn.BooleanType)
      else
        defn.methOfAnyRef(defn.BooleanType)

    sym.copySymDenotation(info = sym.name match
      case DefaultGetterName(nme.copy, n) =>
        transformDefaultGetterCaptures(sym.info, sym.owner, n)
      case nme.unapply =>
        transformUnapplyCaptures(sym.info)
      case nme.apply | nme.copy =>
        if toCC then addCaptureDeps(sym.info) else dropCaptureDeps(sym.info)
      case nme.andThen | nme.compose =>
        transformComposeCaptures(sym)
      case nme.curried | nme.tupled =>
        transformCurriedTupledCaptures(sym)
      case n if n == nme.eq || n == nme.ne =>
        transformCompareCaptures)
  end transform

end Synthetics