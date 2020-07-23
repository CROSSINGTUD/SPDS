package boomerang.scene.jimple;

import boomerang.scene.CallGraph;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootMethod;

public class SootCallGraph extends CallGraph {
  Logger LOGGER = LoggerFactory.getLogger(SootCallGraph.class);

  public SootCallGraph() {
    soot.jimple.toolkits.callgraph.CallGraph callGraph = Scene.v().getCallGraph();
    for (soot.jimple.toolkits.callgraph.Edge e : callGraph) {
      if (e.src().hasActiveBody() && e.tgt().hasActiveBody() && e.srcStmt() != null) {
        Statement[] callSite = JimpleStatement.create(e.srcStmt(), JimpleMethod.of(e.src()));
        if (callSite.length == 2) {
          this.addEdge(new Edge((CallSiteStatement) callSite[0], JimpleMethod.of(e.tgt())));
        } else {
          //                throw new RuntimeException("Excepted to hold a call site reference");
        }
      }
    }
    for (SootMethod m : Scene.v().getEntryPoints()) {
      if (m.hasActiveBody()) this.addEntryPoint(JimpleMethod.of(m));
    }
  }
}
