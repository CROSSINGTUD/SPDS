package test.core.selfrunning;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import boomerang.preanalysis.PreparationTransformer;
import soot.ArrayType;
import soot.G;
import soot.Local;
import soot.Modifier;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;

public abstract class AbstractTestingFramework {
	@Rule
	public TestName testMethodName = new TestName();
	protected SootMethod sootTestMethod;
	protected File ideVizFile;

	@Before
	public void beforeTestCaseExecution() {
		initializeSootWithEntryPoint();
		createVizFile();
		try {
			analyze();
		} catch (ImprecisionException e) {
			Assert.fail(e.getMessage());
		}
		// To never execute the @Test method...
		org.junit.Assume.assumeTrue(false);
	}

	private void createVizFile() {
		ideVizFile = new File(
				"target/IDEViz/" + getTestCaseClassName() + "/IDEViz-" + testMethodName.getMethodName() + ".json");
		if (!ideVizFile.getParentFile().exists()) {
			try {
				Files.createDirectories(ideVizFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException("Was not able to create directories for IDEViz output!");
			}
		}
	}

	private void analyze() {
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.prepare", new PreparationTransformer()));
		Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
		PackManager.v().getPack("wjtp").add(transform);
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
	}

	protected abstract SceneTransformer createAnalysisTransformer() throws ImprecisionException;

	@SuppressWarnings("static-access")
	private void initializeSootWithEntryPoint() {
		G.v().reset();
		Options.v().set_whole_program(true);
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().setPhaseOption("cg.spark", "verbose:true");
		Options.v().set_output_format(Options.output_format_none);
		String userdir = System.getProperty("user.dir");
		String sootCp = userdir + "/target/test-classes";
		if (includeJDK()) {
			String javaHome = System.getProperty("java.home");
			if (javaHome == null || javaHome.equals(""))
				throw new RuntimeException("Could not get property java.home!");
			sootCp += File.pathSeparator + javaHome + "/lib/rt.jar";
			Options.v().setPhaseOption("cg", "trim-clinit:false");
			Options.v().set_no_bodies_for_excluded(true);
			Options.v().set_allow_phantom_refs(true);

			List<String> includeList = new LinkedList<String>();
			includeList.add("java.lang.*");
			includeList.add("java.util.*");
			includeList.add("java.io.*");
			includeList.add("sun.misc.*");
			includeList.add("java.net.*");
			includeList.add("javax.servlet.*");
			includeList.add("javax.crypto.*");

			Options.v().set_include(includeList);

		} else {
			Options.v().set_no_bodies_for_excluded(true);
			Options.v().set_allow_phantom_refs(true);
			// Options.v().setPhaseOption("cg", "all-reachable:true");
		}

		Options.v().set_exclude(excludedPackages());
		Options.v().set_soot_classpath(sootCp);
		// Options.v().set_main_class(this.getTargetClass());
		SootClass sootTestCaseClass = Scene.v().forceResolve(getTestCaseClassName(), SootClass.BODIES);

		for (SootMethod m : sootTestCaseClass.getMethods()) {
			if (m.getName().equals(testMethodName.getMethodName()))
				sootTestMethod = m;
		}
		if (sootTestMethod == null)
			throw new RuntimeException(
					"The method with name " + testMethodName.getMethodName() + " was not found in the Soot Scene.");
		Scene.v().addBasicClass(getTargetClass(), SootClass.BODIES);
		Scene.v().loadNecessaryClasses();
		SootClass c = Scene.v().forceResolve(getTargetClass(), SootClass.BODIES);
		if (c != null) {
			c.setApplicationClass();
		}

		SootMethod methodByName = c.getMethodByName("main");
		List<SootMethod> ePoints = new LinkedList<>();
		ePoints.add(methodByName);
		Scene.v().setEntryPoints(ePoints);
	}

	private String getTargetClass() {
		SootClass sootClass = new SootClass("dummyClass");
		Type paramType = ArrayType.v(RefType.v("java.lang.String"), 1);
		SootMethod mainMethod = new SootMethod("main", Arrays.asList(new Type[] { paramType }), VoidType.v(),
				Modifier.PUBLIC | Modifier.STATIC);
		sootClass.addMethod(mainMethod);
		JimpleBody body = Jimple.v().newBody(mainMethod);
		mainMethod.setActiveBody(body);
		RefType testCaseType = RefType.v(getTestCaseClassName());
		System.out.println(getTestCaseClassName());

		Local loc = Jimple.v().newLocal("l0", paramType);
		body.getLocals().add(loc);
		body.getUnits().add(Jimple.v().newIdentityStmt(loc, Jimple.v().newParameterRef(paramType, 0)));
		Local allocatedTestObj = Jimple.v().newLocal("dummyObj", testCaseType);
		body.getLocals().add(allocatedTestObj);
		body.getUnits().add(Jimple.v().newAssignStmt(allocatedTestObj, Jimple.v().newNewExpr(testCaseType)));
		body.getUnits().add(
				Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(allocatedTestObj, sootTestMethod.makeRef())));
		body.getUnits().add(Jimple.v().newReturnVoidStmt());

		Scene.v().addClass(sootClass);
		body.validate();
		return sootClass.toString();
	}

	private String getTestCaseClassName() {
		return this.getClass().getName().replace("class ", "");
	}

	protected boolean includeJDK() {
		return true;
	}

	public List<String> excludedPackages() {
		List<String> excludedPackages = new LinkedList<>();
		excludedPackages.add("sun.*");
		excludedPackages.add("javax.*");
		excludedPackages.add("com.sun.*");
		excludedPackages.add("com.ibm.*");
		excludedPackages.add("org.xml.*");
		excludedPackages.add("org.w3c.*");
		excludedPackages.add("apple.awt.*");
		excludedPackages.add("com.apple.*");
		return excludedPackages;
	}

	/**
	 * This method can be used in test cases to create branching. It is not
	 * optimized away.
	 * 
	 * @return
	 */
	protected boolean staticallyUnknown() {
		return true;
	}
}
