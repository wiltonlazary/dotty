object A:
  var x = 6

class B(b: Int):
  A.x =  b * 2 // error

object B:
  new B(10)
