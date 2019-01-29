package typestate.dacapo;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Joiner;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class SootSceneSetupDacapo {
	protected Properties benchProperties = new Properties();
	private Properties generalProperties = new Properties();
	private String project;
	private String benchmarkFolder;

	public SootSceneSetupDacapo(String benchmarkFolder, String project) {
		this.benchmarkFolder = benchmarkFolder;
		this.project = project;
		try {
			this.benchLoad();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void benchLoad() throws IOException {
		if (project == null)
			throw new RuntimeException("Set property -Dbenchmark= as VM argument to select the benchmark");
		String propFileName = benchmarkFolder + project + File.separator + project + ".properties";
		InputStream stream = new FileInputStream(propFileName);
		benchProperties.load(stream);
	}

	public void setupSoot() {
		G.v().resetSpark();
		G.v().reset();
		String inputJar = benchProperties.getProperty("input_jar_files");
		String[] split = inputJar.split(":");
		List<String> path = new LinkedList<>();
		for (String spl : split) {
			path.add(prependBasePath(spl));
		}
		String process_dir = Joiner.on(":").join(path);
		String library_jar_files = benchProperties.getProperty("library_jar_files");
		split = library_jar_files.split(":");

		for (String spl : split) {
			path.add(prependBasePath(spl));
		}
		String soot_cp = Joiner.on(":").join(path);
		System.out.println(soot_cp);
//		Options.v().set_soot_classpath(soot_cp);
		Options.v().set_prepend_classpath(true);
		Options.v().set_whole_program(true);
		 Options.v().set_include_all(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_exclude(getExclusions());
		Options.v().set_process_dir(new LinkedList<String>(Collections.singleton(process_dir)));
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_main_class(this.getMainClass());
//		Options.v().setPhaseOption("cg.spark", "on,verbose:true,simulate-natives:true,merge-stringbuffer:false,string-constants:true");
		Options.v().setPhaseOption("cg", "implicit-entry:false,trim-clinit:false");
		Options.v().setPhaseOption("cg.cha", "enabled:true,verbose:true,simulate-natives:false,empties-as-allocs:true,merge-stringbuffer:false,string-constants:true");
//		readDynamicClasses();
		Scene.v().addBasicClass("java.security.Signature",SootClass.HIERARCHY);
		Scene.v().loadNecessaryClasses();
		LinkedList<SootMethod> entryPoint = new LinkedList<>();
		entryPoint.add(Scene.v().getMainMethod());
		Scene.v().setEntryPoints(entryPoint);
		// Scene.v()
		System.out.println(Scene.v().getSootClassPath());
		System.out.println(Scene.v().getEntryPoints());
	}

	private List<String> getExclusions() {
		LinkedList<String> excl = new LinkedList<String>();
		excl.add("COM.rsa.*");
		excl.add("com.ibm.jvm.*");
		excl.add("com.sun.corba.*");
		excl.add("com.sun.net.*");
		excl.add("com.sun.deploy.*");
		excl.add("sun.nio.*");
		excl.add("java.util.logging.*");
		excl.add("sun.util.logging.*");
		excl.add("javax.imageio.*");
		excl.add("javax.swing.*");
		excl.add("sun.swing.*");
		excl.add("java.awt.*");
		excl.add("sun.awt.*");
		excl.add("sun.security.*");
		excl.add("com.sun.*");
		excl.add("sun.*");
		return excl;
	}

	private void readDynamicClasses() {
		List<String> dynClasses = new LinkedList<>();
		String line;
		String propFileName = project + ".dyn";
		try (BufferedReader br = new BufferedReader(new FileReader(new File(prependBasePath(propFileName))))) {
			while ((line = br.readLine()) != null) {
				dynClasses.add(line);
			}
		} catch (IOException | NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Options.v().set_dynamic_class(dynClasses);
		System.out.println(dynClasses);
	}

	private String prependBasePath(String jar) {
		String path = benchmarkFolder + project + "/";
		if (path == null)
			throw new RuntimeException("Set property -DdacapoPath= as VM argument");
		return path + jar;
	}

	protected String getMainClass() {
		return benchProperties.getProperty("main_class");
	}

	public String[] getApplicationClasses() {
		String library_jar_files = benchProperties.getProperty("application_includes");
		String[] split = library_jar_files.split(":");
		return split;
	}

	protected String getBasePath() {
		String path = benchmarkFolder + project + "/";
		if (path == null)
			throw new RuntimeException("Set property -DbenchmarkFolder= as VM argument");
		return path;
	}

	public String getOutputDir() {
		return generalProperties.getProperty("output_dir");
	}

	public String getBenchName() {
		return this.project;
	}
}
