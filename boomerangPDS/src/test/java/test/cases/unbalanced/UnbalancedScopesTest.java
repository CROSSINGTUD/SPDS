package test.cases.unbalanced;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.cases.fields.B;
import test.core.AbstractBoomerangTest;

public class UnbalancedScopesTest extends AbstractBoomerangTest {
  @Test
  public void closingContext() {
    Object object = create();
    queryFor(object);
  }

  @Test
  public void openingContext() {
    Object object = create();
    Object y = object;
    inner(y);
  }

  @Test
  public void doubleClosingContext() {
    Object object = wrappedCreate();
    queryFor(object);
  }

  @Test
  public void branchedReturn() {
    Object object = aOrB();
    queryFor(object);
  }

  @Test
  public void summaryReuse() {
    Object object = createA();
    Object y = object;
    Object x = id(y);
    queryFor(x);
  }

  private Object createA() {
    Alloc c = new Alloc();
    Object d = id(c);
    return d;
  }

  private Object id(Object c) {
    return c;
  }

  private Object aOrB() {
    if (staticallyUnknown()) {
      return new Alloc();
    }
    return new B();
  }

  public Object wrappedCreate() {
    if (staticallyUnknown()) return create();
    return wrappedCreate();
  }

  private void inner(Object inner) {
    Object x = inner;
    queryFor(x);
  }

  private Object create() {
    return new Alloc();
  }
}
