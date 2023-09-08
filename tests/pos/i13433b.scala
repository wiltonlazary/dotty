//> using options -Xfatal-warnings -deprecation -feature

import scala.reflect.ClassTag

type Matcher[A] = A match { case String => String }

def patternMatch[A](a: Any)(using tt: ClassTag[Matcher[A]]): Option[Matcher[A]] = {
  // type T = RDF.Triple[Rdf]
  a match {
    case res: Matcher[A] => Some(res)
    case _ => None
  }
}

def patternMatchWithAlias[A](a: Any)(using tt: ClassTag[Matcher[A]]): Option[Matcher[A]] = {
  type T = Matcher[A]
  a match {
    case res: T => Some(res)
    case _ => None
  }
}


@main def main = {
  println(patternMatch[String]("abc"))
  println(patternMatchWithAlias[String]("abc"))

  println(patternMatch[String](1))
  println(patternMatchWithAlias[String](1))
}
