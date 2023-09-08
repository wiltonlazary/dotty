object C:
  def useR1: Unit =
    // constructor signature
    val r = R1(123, "hello")

    // accessors
    val i: Int = r.i
    val s: String = r.s

    // methods
    val iRes: Int = r.getInt()
    val sRes: String = r.getString()

    // supertype
    val record: java.lang.Record = r

  def useR2: Unit =
    // constructor signature
    val r2 = R2.R(123, "hello")

    // accessors signature
    val i: Int = r2.i
    val s: String = r2.s

    // method
    val i2: Int = r2.getInt

    // supertype
    val isIntLike: IntLike = r2
    val isRecord: java.lang.Record = r2

  def useR3 =
    // constructor signature
    val r3 = R3(123, 42L, "hi")
    new R3("hi", 123)
    // accessors signature
    val i: Int = r3.i
    val l: Long = r3.l
    val s: String = r3.s
    // method
    val l2: Long = r3.l(43L, 44L)
    // supertype
    val isRecord: java.lang.Record = r3
