//> using options -Yno-experimental

import annotation.experimental

@main
@experimental
def run(): Unit = f

@experimental
def f = 2
