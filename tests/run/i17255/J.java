package p;

public class J {
  public static J j = new J();

  public static p.J f() {
    return p.J.j;
  }

  public static Module$ module2() {
    return p.Module$.MODULE$;
  }

  public static p.Module$ module() {
    return p.Module$.MODULE$;
  }

  public static Module.InnerModule$ innermodule2() {
    return p.Module.InnerModule$.MODULE$;
  }

  public static p.Module.InnerModule$ innermodule() {
    return p.Module.InnerModule$.MODULE$;
  }

  public String toString() {
    return "J";
  }
}
