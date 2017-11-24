package dacapo;

import java.io.File;
import java.util.regex.Pattern;

public class FinkOrIDEALDacapoRunner extends ResearchQuestion {
	public static void main(String[] args) throws Exception {
		System.setProperty("analysis", args[0]);
		System.setProperty("rule", args[1]);
		System.setProperty("benchmarkFolder", args[2]);
		System.setProperty("benchmark", args[3]);
		System.setProperty("toCSV", Boolean.toString(true));
		System.setProperty("aliasing", args[5]);
		System.setProperty("strongUpdates", args[6]);
		new FinkOrIDEALDacapoRunner().run(args[4]);
	}

	

	private void run(String numberOfRun) throws Exception {
		String analysis = System.getProperty("analysis");

		String library_jar_files = benchProperties.getProperty("application_includes");
		System.setProperty("application_includes", library_jar_files);
		if(analysis == null)
			throw new RuntimeException("Add -Danalysis to JVM arguments");
		String rule = System.getProperty("rule");
		if(Pattern.matches("PipedInputStream|InputStreamCloseThenRead|OutputStreamCloseThenWrite|PipedOutputStream|PrintStream|PrintWriter", rule)){
			rule = "IO";
		}
		String outputDirectory = "outputDacapo";
		File outputDir = new File(outputDirectory);
		if(!outputDir.exists())
			outputDir.mkdir();
		String outputFile = outputDirectory+File.separator+getMainClass() +"-"+numberOfRun+ "-"+ analysis+"-" +rule +".csv";
		System.setProperty("outputCsvFile", outputFile);
		System.out.println("Writing output to file " +outputFile);
		if(analysis.equalsIgnoreCase("ideal")){
			System.setProperty("rule", Util.selectTypestateMachine(System.getProperty("rule")).getName());
			System.out.println("running " + System.getProperty("rule"));
			System.setProperty("dacapo", "true");
			new IDEALRunner().run();
		} else if(analysis.equalsIgnoreCase("fink-apmust")){
//			TypestateRegressionUnit test = new TypestateRegressionUnit(null, 0);
//			test.selectTypestateRule(System.getProperty("rule"));
//			test.setOption(CommonProperties.Props.MODULES.getName(), getModuleNames());
//			test.setOption(CommonProperties.Props.MAIN_CLASSES.getName(), getMainClass());
//			test.setOption(CommonProperties.Props.TIMEOUT_SECS.getName(), "60000");
//			test.setOption(WholeProgramProperties.Props.CG_KIND.getName(), "ZERO_ONE_CFA");
//
//			test.selectAPMustMustNotTypestateSolver();
//			SafeRegressionDriver.run(test);
		}

	}

	private String getModuleNames() {
		String input_jar_files = getBasePath() + benchProperties.getProperty("input_jar_files");
		String library_jar_files = getBasePath() + benchProperties.getProperty("library_jar_files");
		input_jar_files += ":" + library_jar_files;
		System.out.println(input_jar_files);
		return input_jar_files.replace(":", ",");
	}
}
