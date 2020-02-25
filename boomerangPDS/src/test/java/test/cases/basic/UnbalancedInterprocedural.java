package test.cases.basic;

import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class UnbalancedInterprocedural extends AbstractBoomerangTest {
  @Test
  public void unbalancedCreation() {
    Alloc alias1 = create();
    Alloc query = alias1;
    queryFor(query);
  }

  @Test
  public void doubleUnbalancedCreation() {
    Alloc alias1 = wrappedCreate();
    Alloc query = alias1;
    queryFor(query);
  }

  private Alloc wrappedCreate() {
    return create();
  }

  private Alloc create() {
    return new Alloc();
  }
}
