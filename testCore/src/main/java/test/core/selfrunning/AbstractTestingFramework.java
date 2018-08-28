/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package test.core.selfrunning;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractTestingFramework {
	@Rule
	public TestName testMethodName = new TestName();
	protected SootMethod sootTestMethod;
	protected File ideVizFile;
	protected File dotFile;

	@Before
	public void beforeTestCaseExecution() {
		initializeSootWithEntryPoint();
		createDebugFiles();
		try {
			analyze();
		} catch (ImprecisionException e) {
			Assert.fail(e.getMessage());
		}
		// To never execute the @Test method...
		org.junit.Assume.assumeTrue(false);
	}

	private void createDebugFiles() {
		ideVizFile = new File(
				"target/IDEViz/" + getTestCaseClassName() + "/IDEViz-" + testMethodName.getMethodName() + ".json");
		if (!ideVizFile.getParentFile().exists()) {
			try {
				Files.createDirectories(ideVizFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException("Was not able to create directories for IDEViz output!");
			}
		}
		dotFile = new File(
				"target/dot/" + getTestCaseClassName() + "/Dot-" + testMethodName.getMethodName() + ".dot");
		if (!dotFile.getParentFile().exists()) {
			try {
				Files.createDirectories(dotFile.getParentFile().toPath());
			} catch (IOException e) {
				throw new RuntimeException("Was not able to create directories for dot output!");
			}
		}
	}

	private void analyze() {
		Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
		PackManager.v().getPack("wjtp").add(transform); //whole programm, jimple, user-defined transformations
		PackManager.v().getPack("cg").apply(); //call graph package
		PackManager.v().getPack("wjtp").apply();
	}

	protected abstract SceneTransformer createAnalysisTransformer();

	@SuppressWarnings("static-access")
	private void initializeSootWithEntryPoint() {
		G.v().reset();
		Options.v().set_whole_program(true);

        //https://soot-build.cs.uni-paderborn.de/public/origin/develop/soot/soot-develop/options/soot_options.htm#phase_5_2
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().setPhaseOption("cg.spark", "verbose:true");
		Options.v().set_output_format(Options.output_format_none);

		String userdir = System.getProperty("user.dir");
		String sootCp = userdir + "/target/test-classes";
		String javaHome = System.getProperty("java.home");
		if (javaHome == null || javaHome.equals(""))
			throw new RuntimeException("Could not get property java.home!");
		sootCp += File.pathSeparator + javaHome + "/lib/rt.jar";
		sootCp += File.pathSeparator + javaHome + "/lib/jce.jar";

		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);


		Options.v().set_include(getIncludeList());

		Options.v().setPhaseOption("jb", "use-original-names:true");

		Options.v().set_exclude(excludedPackages());
		Options.v().set_soot_classpath(sootCp);

		SootClass sootTestCaseClass = Scene.v().forceResolve(getTestCaseClassName(), SootClass.BODIES);

		for (SootMethod m : sootTestCaseClass.getMethods()) {
			if (m.getName().equals(testMethodName.getMethodName()))
				sootTestMethod = m;
		}
		if (sootTestMethod == null)
			throw new RuntimeException(
					"The method with name " + testMethodName.getMethodName() + " was not found in the Soot Scene.");
		sootTestMethod.getDeclaringClass().setApplicationClass();
		Scene.v().addBasicClass(getTargetClass(), SootClass.BODIES);
		Scene.v().loadNecessaryClasses();
		SootClass c = Scene.v().forceResolve(getTargetClass(), SootClass.BODIES);
		if (c != null) {
			c.setApplicationClass();
		}
		SootMethod methodByName = c.getMethodByName("main");
		List<SootMethod> ePoints = new LinkedList<>();
		for (SootMethod m : sootTestCaseClass.getMethods()) {
			if (m.isStaticInitializer())
				ePoints.add(m);
		}
		for(SootClass inner : Scene.v().getClasses()){
			if(inner.getName().contains(sootTestCaseClass.getName())){
				for (SootMethod m : inner.getMethods()) {
					if (m.isStaticInitializer())
						ePoints.add(m);
				}
			}
		}
		ePoints.add(methodByName);
		Scene.v().setEntryPoints(ePoints);
	}

	protected List<String> getIncludeList() {
		List<String> includeList = new LinkedList<>();
		includeList.add("java.lang.*");
		includeList.add("java.util.*");
		includeList.add("java.io.*");
		includeList.add("sun.misc.*");
		includeList.add("java.net.*");
		includeList.add("sun.nio.*");
		includeList.add("javax.servlet.*");
		return includeList;
	}

	private String getTargetClass() {
		SootClass sootClass = new SootClass("dummyClass");
		Type paramType = ArrayType.v(RefType.v("java.lang.String"), 1);
		SootMethod mainMethod = new SootMethod("main", Collections.singletonList(paramType), VoidType.v(),
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
