package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class Singleton extends AbstractBoomerangTest {
	private static Alloc instance;
	
	@Test
	public void doubleSingleton(){
		Alloc singleton = Singleton.i();
		Object alias = singleton;
		queryFor(alias);
	}
	@Test
	public void doubleSingletonDirect(){
		Alloc singleton = objectGetter.getG();
		Object alias = singleton;
		queryFor(alias);
	}
	@Test
	public void singletonDirect(){
		Alloc singleton = alloc;
		queryFor(singleton);
	}
	public static Alloc i() {  
			GlobalObjectGetter getter = objectGetter;
			Alloc allocation = getter.getG();
			return allocation; 
	}

    public static interface GlobalObjectGetter {
    	public Alloc getG();
    	public void reset();
    }

	private static Alloc alloc = new Alloc();
	private static GlobalObjectGetter objectGetter = new GlobalObjectGetter() {

        Alloc instance = new Alloc();
        
		public Alloc getG() {
			return instance;
		}

		public void reset() {
			instance = new Alloc();
		}
	};
}
