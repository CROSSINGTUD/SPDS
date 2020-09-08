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

import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.AnalysisScope;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Table;
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
import soot.Transform;
import soot.Transformer;
import soot.options.Options;
import wpds.impl.Weight.NoWeight;

public class ExampleMain2 {
  public static void main(String... args) {
    String sootClassPath = getSootClassPath();
    String mainClass = "boomerang.example.BoomerangExampleTarget2";
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

    // Force resolve inner classes, as the setup does currently not load them automatically.
    c = Scene.v().forceResolve(mainClass + "$NestedClassWithField", SootClass.BODIES);
    c.setApplicationClass();

    c = Scene.v().forceResolve(mainClass + "$ClassWithField", SootClass.BODIES);
    c.setApplicationClass();
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
              protected Collection<? extends Query> generate(Edge cfgEdge) {
                Statement statement = cfgEdge.getStart();
                if (statement.getMethod().toString().contains("ClassWithField")
                    && statement.getMethod().isConstructor()
                    && statement.isAssign()) {
                  if (statement.getRightOp().isIntConstant()) {
                    return Collections.singleton(
                        new ForwardQuery(
                            cfgEdge,
                            new AllocVal(
                                statement.getLeftOp(), statement, statement.getRightOp())));
                  }
                }
                return Collections.emptySet();
              }
            };

        Collection<Query> seeds = scope.computeSeeds();
        for (Query query : seeds) {
          // 1. Create a Boomerang solver.
          Boomerang solver =
              new Boomerang(
                  sootCallGraph, SootDataFlowScope.make(Scene.v()), new DefaultBoomerangOptions());
          System.out.println("Solving query: " + query);
          // 2. Submit a query to the solver.
          ForwardBoomerangResults<NoWeight> forwardBoomerangResults =
              solver.solve((ForwardQuery) query);

          // 3. Process forward results
          Table<Edge, Val, NoWeight> results = forwardBoomerangResults.asStatementValWeightTable();
          for (Edge s : results.rowKeySet()) {
            // 4. Filter results based on your use statement, in our case the call of
            // System.out.println(n.nested.field)
            if (s.getTarget().toString().contains("println")) {
              // 5. Check that a propagated value is used at the particular statement.
              for (Val reachingVal : results.row(s).keySet()) {
                if (s.getTarget().uses(reachingVal)) {
                  System.out.println(query + " reaches " + s);
                }
              }
            }
          }
        }
      }
    };
  }
}
