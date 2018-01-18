package test.cases.threading;

import org.junit.Ignore;
import org.junit.Test;

import test.core.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

@Ignore
public class InnerClassWithThreadTest extends AbstractBoomerangTest {
	private static Allocation param;
	@Test
	public void runWithThread(){
		param = new Allocation();
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
	private class Allocation implements AllocatedObject{
		
	}
	@Override
	protected boolean includeJDK() {
		return true;
	}
}
