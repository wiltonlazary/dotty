//> using options -source 3.7
trait Eq[A]
trait Order[A] extends Eq[A]:
  def toOrdering: Ordering[A]

def Test[Element: Eq: Order] = summon[Eq[Element]].toOrdering // error
