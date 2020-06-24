package test.cases.context;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextTypesTest extends AbstractBoomerangTest {

  @Test
  public void openContext() {
    Alloc alloc = new Alloc();
    call(alloc);
  }

  @Test
  public void twoOpenContexts() {
    Alloc alloc = new Alloc();
    call(alloc);
    Alloc a = new Alloc();
    call(a);
  }

  @Test
  public void twoOpenContextsSameObject() {
    Alloc alloc = new Alloc();
    call(alloc);
    call(alloc);
  }

  private void call(Alloc p) {
    queryFor(p);
  }

  @Test
  public void closingContext() {
    Alloc alloc = close();
    queryFor(alloc);
  }

  private Alloc close() {
    return new Alloc();
  }

  @Test
  public void noContetxt() {
    Alloc alloc = new Alloc();
    queryFor(alloc);
  }

  @Test
  public void twoClosingContexts() {
    Alloc alloc = wrappedClose();
    queryFor(alloc);
  }

  private Alloc wrappedClose() {
    return close();
  }

  @Test
  public void openContextWithField() {
    A a = new A();
    Alloc alloc = new Alloc();
    a.b = alloc;
    call(a);
  }

  private void call(A a) {
    Object t = a.b;
    queryFor(t);
  }

  public static class A {
    Object b = null;
    Object c = null;
  }

  @Test
  public void threeStackedOpenContexts() {
    Alloc alloc = new Alloc();
    wrappedWrappedCall(alloc);
  }

  private void wrappedWrappedCall(Alloc alloc) {
    wrappedCall(alloc);
  }

  private void wrappedCall(Alloc alloc) {
    call(alloc);
  }

  @Test
  public void recursionOpenCallStack() {
    Alloc start = new Alloc();
    recursionStart(start);
  }

  private void recursionStart(Alloc rec) {
    if (staticallyUnknown()) recursionStart(rec);
    call(rec);
  }
}
