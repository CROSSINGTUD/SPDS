package test.cases.callgraph;

import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextSensitivityTest extends AbstractBoomerangTest {

  public void wrongContext() {
    SuperClass type = new WrongSubclass();
    method(type);
  }

  public Object method(SuperClass type) {
    Alloc alloc = new Alloc();
    type.foo(alloc);
    return alloc;
  }

  @Ignore
  @Test
  public void testOnlyCorrectContextInCallGraph() {
    wrongContext();
    SuperClass type = new CorrectSubclass();
    Object alloc = method(type);
    queryFor(alloc);
  }

  public class SuperClass {

    public void foo(Object o) {
      unreachable(o);
    }
  }

  class CorrectSubclass extends SuperClass {
    public void foo(Object o) {}
  }

  class WrongSubclass extends SuperClass {

    public void foo(Object o) {
      unreachable(o);
    }
  }
}
