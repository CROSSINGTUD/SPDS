package test.cases.basic;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class InterprocedualTest extends AbstractBoomerangTest {

	@Test
	public void identityTest() {
		AllocatedObject alias1 = new AllocatedObject(){};
		AllocatedObject alias2 = identity(alias1);
		queryFor(alias2);
	}

	@Test
	public void summaryReuseTest1() {
		AllocatedObject alias1 = new AllocatedObject(){}, alias2, alias3, alias4;
		alias2 = identity(alias1);
		alias3 = identity(alias2);
		alias4 = alias1;
		queryFor(alias4);
	}	
	
	@Test
	public void summaryReuseTest4() {
		Alloc alias2;
//		if(staticallyUnknown()){
			Alloc alias1 = new Alloc();
			alias2 = nestedIdentity(alias1);
//		}else{
//			Alloc alias1 = new Alloc();
//			alias2 = nestedIdentity(alias1);
//		}
		queryFor(alias2);
	}

	private Alloc nestedIdentity(Alloc param2) {
		int shouldNotSeeThis = 1;
		Alloc returnVal = param2;
		return returnVal;
	}

	@Test
	public void summaryReuseTest2() {
		AllocatedObject alias1 = new AllocatedObject(){}, alias2, alias3, alias4;
		alias2 = identity(alias1);
		alias3 = identity(alias2);
		alias4 = alias1;
		queryFor(alias3);
	}

	@Test
	public void summaryReuseTest3() {
		AllocatedObject alias1 = new AllocatedObject(){}, alias2, alias3, alias4;
		alias2 = identity(alias1);
		alias3 = identity(alias2);
		alias4 = alias1;
		queryFor(alias2);
	}
	@Test
	public void interLoop() {
		AllocatedObject alias = new AllocatedObject(){};
		AllocatedObject aliased2;
		Object aliased = new AllocatedObject(){}, notAlias = new Object();
		for (int i = 0; i < 20; i++) {
			aliased = identity(alias);
		}
		aliased2 = (AllocatedObject) aliased;
		queryFor(aliased);
	}

	@Test
	public void wrappedAllocationSite(){
		AllocatedObject alias1 = wrappedCreate();
		queryFor(alias1);
	}

	@Test
	public void branchedObjectCreation() {
		Object alias1;
		if (staticallyUnknown())
			alias1 = create();
		else {
			AllocatedObject intermediate = create();
			alias1 = intermediate;
		}
		Object query = alias1;
		queryFor(query);
	}
	
	@Test
	public void unbalancedCreation() {
		Object alias1 = create();
		Object query = alias1;
		queryFor(query);
	}

	@Test
	public void unbalancedCreationStatic() {
		Object alias1 = createStatic();
		Object query = alias1;
		queryFor(query);
	}

	
	private Object createStatic() {
		return new Allocation();
	}

	public AllocatedObject wrappedCreate() {
		return create();
	}
	public AllocatedObject create() {
		AllocatedObject alloc1 = new AllocatedObject() {
		};
		return alloc1;
	}

	private AllocatedObject identity(AllocatedObject param) {
		AllocatedObject mapped = param;
		return mapped;
	}
}
