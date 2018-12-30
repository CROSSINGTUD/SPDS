package typestate.dacapo;


import java.io.File;
import java.io.IOException;

public class TypestateDacapoAnalysis {

	static String[] dacapo = new String[] { "antlr", "chart", "eclipse", "hsqldb", "jython", "luindex", "lusearch",
			"pmd", "fop","xalan", "bloat" };
	static String[] analyses = new String[] { "ideal"};
	static String[] rules = new String[] { 
	    "IteratorHasNext",
		    "KeyStore",
		    "URLConnection",
		    "InputStreamCloseThenRead",
		    "PipedInputStream",
		    "OutputStreamCloseThenWrite",
		    "PipedOutputStream",
		    "PrintStream",
		    "PrintWriter",
		    "Signature","EmptyVector",	 };
	 
	public static void main(String... args) {
		if(args.length < 1) {
			System.out.println("Please supply path to dacapo benchmark (must end in slash)!");
		}
		for(String analysis : analyses){
			for (String bench : dacapo) {
				for(String rule: rules){
					if(ignore(rule)) {
						continue;
					}
					if(ignore(analysis)) {
						continue;
					}
					if(ignore(bench)) {
						continue;
					}
					String javaHome = System.getProperty("java.home");
			        String javaBin = javaHome +
			                File.separator + "bin" +
			                File.separator + "java";
					ProcessBuilder builder = new ProcessBuilder(new String[] {javaBin, "-Xmx12g","-Xss164m","-cp",  System.getProperty("java.class.path"), IDEALDacapoRunner.class.getName(),analysis, rule, args[0], bench});
					builder.inheritIO();
					Process process;
					try {
						process = builder.start();
						process.waitFor();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static boolean ignore(String rule) {
		return rule.startsWith("#");
	}
}
