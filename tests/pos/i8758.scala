//> using options -Xfatal-warnings -deprecation -feature

def test = "?johndoe" match {
  case s":$name" => println(s":name $name")
  case s"{$name}" =>  println(s"{name} $name")
  case s"?$pos" =>  println(s"pos $pos")
}
