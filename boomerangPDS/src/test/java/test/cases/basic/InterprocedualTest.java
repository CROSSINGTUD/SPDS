package test.cases.basic;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;
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
	public void failedCast() {
		Object o = new Object();
		Object returned = flow(o);
		AllocatedObject t = (AllocatedObject) returned;
		queryFor(t);
	}	
	
	private Object flow(Object o) {
		return o;
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
	
	@Test
	public void branchWithCall(){
		Alloc a1 = new Alloc();
		Alloc a2 = new Alloc();
		Object a = null;
		if(staticallyUnknown()){
			a = a1;
		} else{
			a = a2;
		}
		wrappedFoo(a);
		queryFor(a);
	}
	
	private void wrappedFoo(Object param) {
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
	@Test
	public void heavySumary() {
		Allocation alias1 = new Allocation();
		Object q;
		if(staticallyUnknown()){
			q = doSummarize(alias1);
		}else if(staticallyUnknown()){
			Allocation alias2 = new Allocation();
			q = doSummarize(alias2);
		} else{
			Allocation alias3 = new Allocation();
			q = doSummarize(alias3);
		}
		
		queryFor(q);
	}
	private Allocation doSummarize(Allocation alias1) {
		Allocation a = alias1;
		Allocation b = a;
		Allocation c = b;
		Allocation d = c;
		
		Allocation e = d;
		Allocation f = evenFurtherNested(e);
		Allocation g = alias1;
		if(staticallyUnknown()){
			g = f;
		}
		Allocation h = g;
		return f;
	}
	
	private Allocation evenFurtherNested(Allocation e) {
		return e;
	}

	@Test
	public void summaryTest() {
		Allocation alias1 = new Allocation();
		Object q;
		if(staticallyUnknown()){
			q = summary(alias1);
		}else{
			Allocation alias2 = new Allocation();
			q = summary(alias2);
		}
		
		queryFor(q);
	}

	private Object summary(Allocation inner) {
		Allocation ret = inner;
		System.out.println(1);
		return ret;
	}
	
	@Test
	public void doubleNestedSummary() {
		Allocation alias1 = new Allocation();
		Object q;
		if(staticallyUnknown()){
			q = nestedSummary(alias1);
		}else{
			Allocation alias2 = new Allocation();
			q = nestedSummary(alias2);
		}
		
		queryFor(q);
	}

	private Object nestedSummary(Allocation inner) {
		Object ret = summary(inner);
		System.out.println(1);
		return ret;
	}
}
