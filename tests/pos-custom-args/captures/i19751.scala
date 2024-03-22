import language.experimental.captureChecking
import annotation.capability
import caps.cap

trait Ptr[A]
@capability trait Scope:
  def allocate(size: Int): Ptr[Unit]^{this}


object Scope:
  def confined[A](fn: Scope ?->{cap} A): A =
    val scope = new Scope:
      def allocate(size: Int) = new Ptr[Unit] {}
    fn(using scope)

def Test: Unit =
  val s = Scope.confined:
    val s2 = summon[Scope]
    Scope.confined:
      s2.allocate(5)
    5

