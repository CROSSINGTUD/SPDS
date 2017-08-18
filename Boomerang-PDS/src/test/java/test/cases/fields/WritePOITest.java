package test.cases.fields;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class WritePOITest extends AbstractBoomerangTest {
	private class A{
		Alloc b = null;
		Alloc c = null;
	}
	
	
	@Test
	public void indirectAllocationSite(){
		Alloc query = new Alloc();
		A a = new A();
		A e = a;
		a.b = query;
		Alloc alias = e.b;
		queryFor(alias);
	}

	private class Level1{
		Level2 l2;
	}
	private class Level2{
		Alloc a;
	}
	
	@Test
	public void doubleIndirectAllocationSite(){
		Level1 base = new Level1();
		
		Alloc query = new Alloc();
		Level2 level2 = new Level2();
		base.l2 = level2;
		level2.a = query;
		Alloc samesame = base.l2.a;
		queryFor(samesame);
	}
	
	
	@Test
	public void doubleIndirectAllocationSiteMoreComplex(){
		Level1 base = new Level1();
		Level1 baseAlias = base;
		
		Alloc query = new Alloc();
		Level2 level2 = new Level2();
		base.l2 = level2;
		level2.a = query;
		Alloc samesame = baseAlias.l2.a;
		queryFor(samesame);
	}
	
	
	@Test
	public void directAllocationSite(){
		Alloc query = new Alloc();
		A a = new A();
		a.b = query;
		Alloc alias = a.b;
		queryFor(alias);
	}
	private class Alloc implements AllocatedObject{};
}
