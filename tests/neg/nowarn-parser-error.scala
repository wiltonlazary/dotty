//> using options -deprecation -Wunused:nowarn

class C {
  def a = try 1 // warn
  def def // error
}
