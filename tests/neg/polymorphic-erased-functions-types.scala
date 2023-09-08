import language.experimental.erasedDefinitions

def t1a: [T] => T => Unit = [T] => (erased t: T) => () // error
def t1b: [T] => (erased T) => Unit = [T] => (t: T) => () // error

def t2a: [T, U] => (T, U) => Unit = [T, U] => (t: T, erased u: U) => () // error
def t2b: [T, U] => (T, erased U) => Unit = [T, U] => (t: T, u: U) => () // error
