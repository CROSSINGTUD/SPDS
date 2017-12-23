package test.cases.array;

import org.junit.Ignore;
import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;


@Ignore
public class ArrayContainerTest extends AbstractBoomerangTest {

	
	private static class ArrayContainer{
		AllocatedObject[] array = new AllocatedObject[]{};
		void put(Object o){
			array[0] = (AllocatedObject) o;
		}
		AllocatedObject get(){
			return array[0];
		}
	}
	
	@Test
	public void insertAndGet(){
		ArrayContainer container = new ArrayContainer();
		Object o1 = new Object();
		container.put(o1);
		AllocatedObject o2 = new Alloc();
		container.put(o2);
		AllocatedObject alias = container.get();
		queryFor(alias);
	}
	
	@Test
	public void insertAndGetDouble(){
		ArrayOfArrayOfContainers outerContainer = new ArrayOfArrayOfContainers();
		ArrayContainer container = new ArrayContainer();
		Object o1 = new Object();
		container.put(o1);
		AllocatedObject o2 = new Alloc();
		container.put(o2);
		outerContainer.put(container);
		ArrayContainer aliasContainer = outerContainer.get();
		AllocatedObject alias = aliasContainer.get();
		queryFor(alias);
	}

	private static class ArrayOfArrayOfContainers{
		ArrayContainer[] array = new ArrayContainer[]{};
		void put(ArrayContainer o){
			array[0] = o;
		}
		ArrayContainer get(){
			return array[0];
		}
	}
}
