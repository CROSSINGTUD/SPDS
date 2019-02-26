package typestate;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

import experiments.google.spreadsheet.GoogleSpreadsheetWriter;
import reachability.DacapoReachabilityAnalysis;

@RunWith(Parameterized.class)
public class ReachabilityTest {

	@BeforeClass
	public static void setup() {
//		try {
////			GoogleSpreadsheetWriter
////					.createSheet(Arrays.asList(new String[] { "Analysis", "Program", "Rule", "Seed", "SeedStatement",
////							"SeedMethod", "SeedClass", "Is_In_Error", "Timedout", "AnalysisTimes", "PropagationCount",
////							"VisitedMethod", "ReachableMethods", "CallRecursion", "FieldLoop", "MaxMemory" }));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (GeneralSecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@AfterClass
	public static void shutdown() throws IOException, GeneralSecurityException {
		GoogleSpreadsheetWriter.computeMetrics();
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		String[] dacapo = new String[] { "antlr", "chart", "eclipse", "hsqldb", "jython", "luindex", "lusearch",
				"pmd", "fop", "xalan", "bloat" };
		ArrayList<Object[]> res = Lists.newArrayList();
		ArrayList<Object[]> res2 = Lists.newArrayList();
		for (String prog : dacapo) {
			if (ignore(prog))
				continue;
			res.add(new Object[] { prog });
		}
		res.addAll(res2);
		return res;
	}

	private static boolean ignore(String prop) {
		return prop.startsWith("#");
	}

	private String prog;

	public ReachabilityTest(String prog) {
        this.prog = prog;
    }

	@Test
	public void test() throws Exception {
		String userDir = System.getProperty("user.dir");
		String dacapoPath = userDir + File.separator + "dacapo" + File.separator;
		System.setProperty("demandDrivenCg", "true");
		DacapoReachabilityAnalysis an = new DacapoReachabilityAnalysis(dacapoPath, prog);
		an.run();
	}
}
