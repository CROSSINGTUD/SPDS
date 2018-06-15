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
package boomerang.example;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;
import soot.util.queue.QueueReader;
import wpds.impl.Weight.NoWeight;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExampleMain {
	public static void main(String... args) {
		String sootClassPath = getSootClassPath();
		String mainClass = "boomerang.example.BoomerangExampleTarget";
		setupSoot(sootClassPath, mainClass);
		analyze();
	}

	private static String getSootClassPath(){
		//Assume target folder to be directly in user dir; this should work in eclipse
		String sootClassPath = System.getProperty("user.dir") + File.separator+"target"+File.separator+"classes";
		File classPathDir = new File(sootClassPath);
		if (!classPathDir.exists()){
			//We haven't found our target folder
			//Check if if it is in the boomerangPDS in user dir; this should work in IntelliJ
			sootClassPath = System.getProperty("user.dir") + File.separator + "boomerangPDS"+ File.separator+
					"target"+File.separator+"classes";
			classPathDir = new File(sootClassPath);
			if (!classPathDir.exists()){
				//We haven't found our bytecode anyway, notify now instead of starting analysis anyway
				throw new RuntimeException("Classpath could not be found.");
			}
		}
		return sootClassPath;
	}

	private static void setupSoot(String sootClassPath, String mainClass) {
		G.v().reset();
		Options.v().set_whole_program(true);
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().set_output_format(Options.output_format_none);
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
		Options.v().setPhaseOption("jb", "use-original-names:true");

		Options.v().set_soot_classpath(sootClassPath);
		Options.v().set_prepend_classpath(true);
		// Options.v().set_main_class(this.getTargetClass());
		Scene.v().loadNecessaryClasses();
		SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
		if (c != null) {
			c.setApplicationClass();
		}
		for(SootMethod m : c.getMethods()){
			System.out.println(m);
		}
	}
	private static void analyze() {
		Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
		PackManager.v().getPack("wjtp").add(transform);
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
	}

	private static Transformer createAnalysisTransformer() {
		return new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {

				//1. Create a Boomerang solver.
				Boomerang solver = new Boomerang(new DefaultBoomerangOptions(){
					@Override
					public boolean onTheFlyCallGraph() {
						//Must be turned off if no SeedFactory is specified.
						return false;
					}
				}) {
					ObservableICFG<Unit,SootMethod> icfg = new ObservableDynamicICFG(this);
					@Override
					public ObservableICFG<Unit, SootMethod> icfg() {
						return icfg;
					}
					
					@Override
					public SeedFactory<NoWeight> getSeedFactory() {
						return null;
					}
				};

				//2. Submit a query to the solver.
				BackwardQuery query = createQuery();
				System.out.println("Solving query: " + query);
				BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
				solver.debugOutput();
				System.out.println("All allocation sites of the query variable are:");
				System.out.println(backwardQueryResults.getAllocationSites());

				System.out.println("All aliasing access path of the query variable are:");
				System.out.println(backwardQueryResults.getAllAliases());
			}

			private BackwardQuery createQuery() {
				ReachableMethods reachableMethods = Scene.v().getReachableMethods();
				QueueReader<MethodOrMethodContext> l = reachableMethods.listener();
				while(l.hasNext()){
					MethodOrMethodContext next = l.next();
					BackwardQuery q = getQuery(next.method());
					if(q != null)
						return q;
				}
				throw new RuntimeException("No method found that contains a call to method queryFor!");
			}

			private BackwardQuery getQuery(SootMethod method) {
				if(!method.hasActiveBody()){
					return null;
				}
				for(Unit u :  method.getActiveBody().getUnits()){
					if(!(u instanceof Stmt)){
						continue;
					}
					Stmt s = (Stmt) u;
					if(!s.containsInvokeExpr()){
						continue;
					}
					if(s.toString().contains("queryFor")){
						Value arg = s.getInvokeExpr().getArg(0);
						return new BackwardQuery(new Statement(s,method), new Val(arg, method));
					}
				}
				return null;
			}
		};
	}

}
