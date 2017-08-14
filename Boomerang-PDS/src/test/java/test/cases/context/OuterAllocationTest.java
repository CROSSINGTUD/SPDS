package test.cases.context;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class OuterAllocationTest extends AbstractBoomerangTest{
	@Test
	public void main() {
	    ObjectWithField container = new ObjectWithField();
	    container.field = new File();
	    ObjectWithField otherContainer = new ObjectWithField();
	    File a = container.field;
	    otherContainer.field = a;
	    flows(container);
	  }

	  private void flows(ObjectWithField container) {
	    File field = container.field;
	    field.open();
	    queryFor(field);
	  }
	  
	  private static class File implements AllocatedObject{
		  public void open(){}
	  }
	  private static class ObjectWithField{
		  File field;
	  }
}
