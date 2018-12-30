package typestate.dacapo;


import java.io.File;
import java.util.regex.Pattern;

public class IDEALDacapoRunner extends SootSceneSetupDacapo {
	private static String project;
	private static String benchFolder;

	public IDEALDacapoRunner(String benchmarkFolder, String benchFolder) {
		super(benchmarkFolder, benchFolder);
	}



	public static void main(String[] args) throws Exception {
		System.setProperty("analysis", args[0]);
		System.setProperty("rule", args[1]);
		benchFolder = args[2];
		project =  args[3];
		new IDEALDacapoRunner(benchFolder,project).run();
	}

	

	private void run() throws Exception {
		String analysis = System.getProperty("analysis");

		String library_jar_files = benchProperties.getProperty("application_includes");
		System.setProperty("application_includes", library_jar_files);
		if(analysis == null)
			throw new RuntimeException("Add -Danalysis to JVM arguments");
		String rule = System.getProperty("rule");
		if(Pattern.matches("PipedInputStream|InputStreamCloseThenRead|OutputStreamCloseThenWrite|PipedOutputStream|PrintStream|PrintWriter", rule)){
			rule = "IO";
		}
		System.setProperty("ruleIdentifier",rule);
		String outputDirectory = "outputDacapo";
		File outputDir = new File(outputDirectory);
		if(!outputDir.exists())
			outputDir.mkdir();
		String outputFile = outputDirectory+File.separator+getMainClass() +"-"+analysis+"-" + rule+".csv";
		System.setProperty("outputCsvFile", outputFile);
		
		System.out.println("Writing output to file " +outputFile);
		if(analysis.equalsIgnoreCase("ideal")){
			System.setProperty("rule", Util.selectTypestateMachine(System.getProperty("rule")).getName());
			System.out.println("running " + System.getProperty("rule"));
			System.setProperty("dacapo", "true");
			new IDEALRunner(benchFolder,project).run(outputFile);
		}

	}

	private String getModuleNames() {
		String input_jar_files = getBasePath() + benchProperties.getProperty("input_jar_files");
		String library_jar_files = getBasePath() + benchProperties.getProperty("library_jar_files");
		input_jar_files += File.pathSeparator + library_jar_files;
		System.out.println(input_jar_files);
		return input_jar_files.replace(":", ",");
	}
}
