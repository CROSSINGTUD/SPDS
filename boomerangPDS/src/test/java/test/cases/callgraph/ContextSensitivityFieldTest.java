package test.cases.callgraph;

import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextSensitivityFieldTest extends AbstractBoomerangTest {

  public void wrongContext() {
    SuperClass type = new WrongSubclass();
    method(type);
  }

  public Object method(SuperClass type) {
    Alloc alloc = new Alloc();
    type.foo(alloc);
    return type.getO();
  }

  // Method WrongSubclass.foo(Object o) is incorrectly marked as reachable.
  @Ignore
  @Test
  public void testOnlyCorrectContextInCallGraph() {
    wrongContext();
    SuperClass type = new CorrectSubclass();
    Object alloc = method(type);
    queryFor(alloc);
  }

  public static class SuperClass {
    Object o;

    public void foo(Object o) {
      this.o = o;
    }

    public Object getO() {
      return o;
    }
  }

  static class CorrectSubclass extends SuperClass {

    public void foo(Object o) {
      super.foo(o);
    }
  }

  static class WrongSubclass extends SuperClass {

    public void foo(Object o) {
      unreachable(o);
    }

    public void unreachable(Object o) {}
  }
}
