//> using options -Xfatal-warnings

def f(): Unit = {
  () // error
  ()
}

inline def g(): Unit = {
  () // error
  ()
}
