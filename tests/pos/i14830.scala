
// https://github.com/lampepfl/dotty/issues/14830
val a: Comparable[String] = "Fred"
val b: { def length: Int } = "Fred"
val c: Comparable[String] & { def length: Int } = "Fred"
val d: Comparable[String] & { def length(): Int } = "Fred"
