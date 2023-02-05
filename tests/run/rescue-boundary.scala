import scala.util.control.NonFatal
import scala.util.boundary, boundary.break

object lib:
  extension [T](op: => T) inline def rescue (fallback: => T) =
    try op
    catch
      case ex: boundary.Break[_] => throw ex
      case NonFatal(_) => fallback

  extension [T, E <: Throwable](op: => T) inline def rescue (fallback: PartialFunction[E, T]) =
    try op
    catch
      case ex: E =>
        // user should never match `ReturnThrowable`, which breaks semantics of non-local return
        if fallback.isDefinedAt(ex) && !ex.isInstanceOf[boundary.Break[_]] then fallback(ex) else throw ex
end lib

import lib.*

@main def Test = {
  assert((9 / 1 rescue 1) == 9)
  assert((9 / 0 rescue 1) == 1)
  assert(((9 / 0 rescue { case ex: NullPointerException => 5  }) rescue 10) == 10)
  assert(((9 / 0 rescue { case ex: ArithmeticException => 5  }) rescue 10) == 5)

  assert(
    {
      9 / 0 rescue {
        case ex: NullPointerException => 4
        case ex: ArithmeticException => 3
      }
    } == 3
  )

  (9 / 0) rescue { case ex: ArithmeticException => 4 }

  assert(
    {
      {
        val a = 9 / 0 rescue {
          case ex: NullPointerException => 4
        }
        a * a
      } rescue {
        case ex: ArithmeticException => 3
      }
    } == 3
  )

  assert(foo(10) == 40)
  assert(bar(10) == 40)

  // should not catch fatal errors
  assert(
    try { { throw new OutOfMemoryError(); true } rescue false }
    catch { case _: OutOfMemoryError => true }
  )

  // should catch any errors specified, including fatal errors
  assert(
    try { { throw new OutOfMemoryError(); true } rescue { case _: OutOfMemoryError => true } }
    catch { case _: OutOfMemoryError => false }
  )

  // should not catch NonLocalReturns
  def foo(x: Int): Int = boundary {
    { break[Int](4 * x) : Int } rescue 10
  }

  // should catch specified exceptions, but not NonLocalReturn
  def bar(x: Int): Int = boundary {
    { break[Int](4 * x) : Int } rescue { case _ => 10 }
  }

}
