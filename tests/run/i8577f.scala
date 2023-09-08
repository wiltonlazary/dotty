object Macro:
  opaque type StrCtx = StringContext
  def apply(ctx: StringContext): StrCtx = ctx
  def unapply(ctx: StrCtx): Option[StringContext] = Some(ctx)

@main def Test: Unit =
  extension (ctx: StringContext) def mac: Macro.StrCtx = Macro(ctx)
  extension (inline ctx: Macro.StrCtx) inline def unapplySeq[U](inline input: U): Option[Seq[U]] =
    Some(Seq(input))

  val mac"$x" = 1
  val y: Int = x
  assert(x == 1)
