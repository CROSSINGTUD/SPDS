package typestate;

import java.io.File;

import org.junit.Test;

import typestate.dacapo.TypestateDacapoAnalysis;

public class PerformanceTest {

	@Test
	public void test() {
		String userDir = System.getProperty("user.dir");
		TypestateDacapoAnalysis.main(new String[] {userDir+File.separator+"dacapo"+File.separator});
	}
}
