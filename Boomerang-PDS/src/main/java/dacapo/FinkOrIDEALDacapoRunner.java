package dacapo;

public class FinkOrIDEALDacapoRunner extends ResearchQuestion {
	public static void main(String[] args) throws Exception {
		System.setProperty("benchmarkFolder", args[0]);
		System.setProperty("benchmark", args[1]);
		new FinkOrIDEALDacapoRunner().run();
	}

	

	private void run() throws Exception {
		String library_jar_files = benchProperties.getProperty("application_includes");
		System.setProperty("application_includes", library_jar_files);
		System.out.println("running " + System.getProperty("rule"));
		new IDEALRunner().run();

	}

	private String getModuleNames() {
		String input_jar_files = getBasePath() + benchProperties.getProperty("input_jar_files");
		String library_jar_files = getBasePath() + benchProperties.getProperty("library_jar_files");
		input_jar_files += ":" + library_jar_files;
		System.out.println(input_jar_files);
		return input_jar_files.replace(":", ",");
	}
}
