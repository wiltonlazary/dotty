import scala.language.experimental.clauseInterleaving

object newline {
  def multipleLines
        [T]
        (x: T)
        [U]
        (using (T,U))
        (y: U)
        = ???
}
