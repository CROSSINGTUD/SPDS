package boomerang.scene.jimple;

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Statement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class JimpleControlFlowGraph implements ControlFlowGraph {

  private UnitGraph graph;

  private boolean cacheBuild = false;
  private List<Statement> startPointCache = Lists.newArrayList();
  private List<Statement> endPointCache = Lists.newArrayList();
  private Multimap<Statement, Statement> succsOfCache = HashMultimap.create();
  private Multimap<Statement, Statement> predsOfCache = HashMultimap.create();
  private List<Statement> statements = Lists.newArrayList();;

  private JimpleMethod method;

  public JimpleControlFlowGraph(JimpleMethod method) {
    this.method = method;
    this.graph = new BriefUnitGraph(method.getDelegate().getActiveBody());
  }

  public Collection<Statement> getStartPoints() {
    buildCache();
    return startPointCache;
  }

  private void buildCache() {
    if (cacheBuild) return;
    cacheBuild = true;
    List<Unit> heads = graph.getHeads();
    for (Unit u : heads) {
      // We add a nop statement to the body and ignore IdentityStmt ($stack14 := @caughtexception)
      if (u instanceof IdentityStmt) {
        continue;
      }
      Statement stmt = JimpleStatement.create((Stmt) u, method);
      startPointCache.add(stmt);
    }
    List<Unit> tails = graph.getTails();
    for (Unit u : tails) {
      Statement stmt = JimpleStatement.create((Stmt) u, method);
      endPointCache.add(stmt);
    }

    UnitPatchingChain units = method.getDelegate().getActiveBody().getUnits();
    for (Unit u : units) {
      Statement first = JimpleStatement.create((Stmt) u, method);
      statements.add(first);

      for (Unit succ : graph.getSuccsOf(u)) {
        Statement succStmt = JimpleStatement.create((Stmt) succ, method);
        succsOfCache.put(first, succStmt);
      }

      for (Unit pred : graph.getPredsOf(u)) {
        Statement predStmt = JimpleStatement.create((Stmt) pred, method);
        predsOfCache.put(first, predStmt);
      }
    }
  }

  public Collection<Statement> getEndPoints() {
    buildCache();
    return endPointCache;
  }

  public Collection<Statement> getSuccsOf(Statement curr) {
    buildCache();
    return succsOfCache.get(curr);
  }

  public Collection<Statement> getPredsOf(Statement curr) {
    buildCache();
    return predsOfCache.get(curr);
  }

  public List<Statement> getStatements() {
    buildCache();
    return statements;
  }
}
