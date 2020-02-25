package test.cases.callgraph;

import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class ContextSpecificListTypeTest extends AbstractBoomerangTest {

  public void wrongContext() {
    List list = new WrongList();
    method(list);
  }

  public Object method(List list) {
    Alloc alloc = new Alloc();
    list.add(alloc);
    return alloc;
  }

  @Ignore
  @Test
  public void testListType() {
    wrongContext();
    List list = new ArrayList();
    Object query = method(list);
    queryFor(query);
  }

  private static class WrongList extends LinkedList {
    @Override
    public boolean add(Object e) {
      unreachable();
      return false;
    }

    public void unreachable() {}
  }

  @Override
  protected BoomerangOptions createBoomerangOptions() {
    return new DefaultBoomerangOptions() {

      @Override
      public boolean onTheFlyCallGraph() {
        return true;
      }
    };
  }
}
