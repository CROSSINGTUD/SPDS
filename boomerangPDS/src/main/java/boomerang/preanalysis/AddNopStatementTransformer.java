package boomerang.preanalysis;

import java.util.Map;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.internal.JNopStmt;

public class AddNopStatementTransformer extends SceneTransformer {

  @Override
  protected void internalTransform(String phaseName, Map<String, String> options) {
    for (SootClass c : Scene.v().getClasses()) {
      for (SootMethod m : c.getMethods()) {
        if (!m.hasActiveBody()) {
          continue;
        }
        Body b = m.getActiveBody();
        b.getUnits().addFirst(new JNopStmt());
      }
    }
  }

}
