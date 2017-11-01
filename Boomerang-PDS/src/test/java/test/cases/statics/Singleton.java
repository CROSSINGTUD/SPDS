package test.cases.statics;

import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class Singleton extends AbstractBoomerangTest {
	private static Object alloc;
	private static Alloc instance;
	
	@Test
	public void doubleSingleton(){
		Alloc singleton = Singleton.i();
		Object alias = singleton;
		queryFor(alias);
	}
	public static Alloc i() { return objectGetter.getG(); }

    public static interface GlobalObjectGetter {
    	public Alloc getG();
    	public void reset();
    }
	private static GlobalObjectGetter objectGetter = new GlobalObjectGetter() {

        private Alloc instance = new Alloc();
        
		public Alloc getG() {
			return instance;
		}

		public void reset() {
			instance = new Alloc();
		}
	};
}
