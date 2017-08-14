package test.cases.subclassing;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class SubclassingTest extends AbstractBoomerangTest{
	private static class Superclass{
		AllocatedObject o = new AllocatedObject(){};
	}
	
	private static class Subclass extends Superclass{
		
	}
	
	private static class ClassWithSubclassField{
		Subclass f;
		public ClassWithSubclassField(Subclass t){
			this.f = t;
		}
	}
	
	@Test
	public void typingIssue(){
		Subclass subclass = new Subclass();
		ClassWithSubclassField classWithSubclassField = new ClassWithSubclassField(subclass);
		AllocatedObject query = classWithSubclassField.f.o;
		queryFor(query);
	}
	
	
	
}
