package dacapo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
	// jython and haqldb fail on WALA (no callgraph can be computed)
	static String[] dacapo = new String[] { "antlr", "chart", "eclipse",
			/* "jython","hsqldb", */"luindex", "lusearch", "pmd", /* "xalan", */ "bloat" };
//	static String[] dacapo = new String[] {  "bloat" };
static String[] rules = new String[] { "EmptyVector" };
//	static String[] rules = new String[] { "EmptyVector", "IteratorHasNext",
//			/*
//			 * "KeyStore", //No seed to Signature found in the dacapo programs
//			 * we analyzed "URLConnection",
//			 */ // No seed to Signature found in the dacapo programs we analyzed
//			"InputStreamCloseThenRead", "PipedInputStream", "OutputStreamCloseThenWrite", "PipedOutputStream",
//			"PrintStream", "PrintWriter"/*
//										 * , "Signature"
//										 */ }; // No seed to Signature found in
//												// the dacapo programs we
//												// analyzed

	static String benchmarkFolder = "/Users/johannesspath/Documents/dacapo/";
	private final static int NUMBER_OF_ITERATIONS = 1;

	public static void main(String... args) {
		for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
			runAnalysis("ideal","run-"+i,true,true);
			//runAnalysis("fink-apmust","run-"+i,true,true);
		}

		runAnalysis("ideal","noAliasing",false,true);
		runAnalysis("ideal","noStrongUpdates",true,false);
	}

	private static void runAnalysis(String analysis, String fileSuffix, boolean aliasing, boolean strongUpdates) {
		//Just copy pasted here
		for (String rule : rules) {
			for (String bench : dacapo) {
				String javaHome = System.getProperty("java.home");
				String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
				String[] args = new String[] { javaBin, "-Xmx14g", "-Xms14g", "-Xss64m", "-cp",
						System.getProperty("java.class.path"), FinkOrIDEALDacapoRunner.class.getName(), analysis, rule,
						benchmarkFolder, bench, fileSuffix, /* aliasing */Boolean.toString(aliasing), /* strongUpdates */ Boolean.toString(strongUpdates) };
				System.out.println(Arrays.toString(args));
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.inheritIO();

				Process process;
				try {
					process = builder.start();
					process.waitFor();
				} catch (IOException | InterruptedException e) {
				}
			}
		}
	}

}
