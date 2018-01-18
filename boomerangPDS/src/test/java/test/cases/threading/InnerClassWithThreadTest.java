package test.cases.threading;

import org.junit.Ignore;
import org.junit.Test;

import test.cases.fields.Alloc;
import test.core.AbstractBoomerangTest;

public class InnerClassWithThreadTest extends AbstractBoomerangTest {
	private static Alloc param;
	@Ignore
	@Test
	public void runWithThreadStatic(){
		param = new Alloc();
		Runnable r = new Runnable(){

			@Override
			public void run() {
				String cmd = System.getProperty("");
//				if(cmd!=null){
//					param = new Allocation();
//				}
				for(int i = 1; i < 3; i++){
					queryFor(param);
				}
			}
			
		};
		Thread t = new Thread(r);
		t.start();
	}
	@Ignore
	@Test
	public void runWithThread(){
		final Alloc u = new Alloc();
		Runnable r = new Runnable(){

			@Override
			public void run() {
				String cmd = System.getProperty("");
//				if(cmd!=null){
//					param = new Allocation();
//				}
				for(int i = 1; i < 3; i++){
					queryFor(u);
				}
			}
			
		};
		Thread t = new Thread(r);
		t.start();
	}
	
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
