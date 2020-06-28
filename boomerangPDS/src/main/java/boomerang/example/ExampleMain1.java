/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.example;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.Query;
import boomerang.results.BackwardBoomerangResults;
import boomerang.scene.AnalysisScope;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.SootCallGraph;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Transformer;
import soot.options.Options;
import wpds.impl.Weight;

public class ExampleMain1 {
  public static void main(String... args) {
    String sootClassPath = getSootClassPath();
    String mainClass = "boomerang.example.BoomerangExampleTarget1";
    setupSoot(sootClassPath, mainClass);
    analyze();
  }

  private static String getSootClassPath() {
    // Assume target folder to be directly in user dir; this should work in eclipse
    String sootClassPath =
        System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes";
    File classPathDir = new File(sootClassPath);
    if (!classPathDir.exists()) {
      // We haven't found our target folder
      // Check if if it is in the boomerangPDS in user dir; this should work in IntelliJ
      sootClassPath =
          System.getProperty("user.dir")
              + File.separator
              + "boomerangPDS"
              + File.separator
              + "target"
              + File.separator
              + "classes";
      classPathDir = new File(sootClassPath);
      if (!classPathDir.exists()) {
        // We haven't found our bytecode anyway, notify now instead of starting analysis anyway
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
    for (SootMethod m : c.getMethods()) {
      System.out.println(m);
    }
  }

  private static void analyze() {
    Transform transform = new Transform("wjtp.ifds", createAnalysisTransformer());
    PackManager.v().getPack("wjtp").add(transform);
    PackManager.v().getPack("cg").apply();
    BoomerangPretransformer.v().apply();
    PackManager.v().getPack("wjtp").apply();
  }

  private static Transformer createAnalysisTransformer() {
    return new SceneTransformer() {
      protected void internalTransform(
          String phaseName, @SuppressWarnings("rawtypes") Map options) {
        SootCallGraph sootCallGraph = new SootCallGraph();
        AnalysisScope scope =
            new AnalysisScope(sootCallGraph) {
              @Override
              protected Collection<? extends Query> generate(Statement statement) {
                if (statement.toString().contains("queryFor") && statement.containsInvokeExpr()) {
                  Val arg = statement.getInvokeExpr().getArg(0);
                  return Collections.singleton(BackwardQuery.make(statement, arg));
                }
                return Collections.emptySet();
              }
            };
        // 1. Create a Boomerang solver.
        Boomerang solver =
            new Boomerang(
                sootCallGraph, SootDataFlowScope.make(Scene.v()), new DefaultBoomerangOptions());

        // 2. Submit a query to the solver.
        Collection<Query> seeds = scope.computeSeeds();
        for (Query query : seeds) {
          System.out.println("Solving query: " + query);
          BackwardBoomerangResults<Weight.NoWeight> backwardQueryResults =
              solver.solve((BackwardQuery) query);
          System.out.println("All allocation sites of the query variable are:");
          System.out.println(backwardQueryResults.getAllocationSites());

          System.out.println("All aliasing access path of the query variable are:");
          System.out.println(backwardQueryResults.getAllAliases());
        }
      }
    };
  }
}
