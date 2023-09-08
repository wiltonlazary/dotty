package dotty.tools.pc.utils

import scala.meta.pc.VirtualFileParams

import dotty.tools.dotc.ast.tpd.*
import dotty.tools.dotc.ast.{Trees, tpd}
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.dotc.util.SourcePosition
import dotty.tools.pc.PcCollector

final class DefSymbolCollector(
    driver: InteractiveDriver,
    params: VirtualFileParams
) extends PcCollector[Option[Symbol]](driver, params):

  def collect(parent: Option[Tree])(
      tree: Tree,
      toAdjust: SourcePosition,
      sym: Option[Symbol]
  ): Option[Symbol] =
    tree match
      case tree: NamedDefTree => Some(tree.symbol)
      case _ => None

  def namedDefSymbols: List[(Symbol, String)] =
    result().flatten.map(symbol => symbol -> symbol.name.toString)
