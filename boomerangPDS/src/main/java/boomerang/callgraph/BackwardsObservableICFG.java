package boomerang.callgraph;

import boomerang.scene.CallGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import java.util.Collection;

public class BackwardsObservableICFG implements ObservableICFG<Statement, Method> {
  protected final ObservableICFG<Statement, Method> delegate;

  public BackwardsObservableICFG(ObservableICFG<Statement, Method> fwOICFG) {
    this.delegate = fwOICFG;
  }

  @Override
  public Collection<Statement> getStartPointsOf(Method m) {
    return this.delegate.getEndPointsOf(m);
  }

  @Override
  public boolean isExitStmt(Statement stmt) {
    return this.delegate.isStartPoint(stmt);
  }

  @Override
  public boolean isStartPoint(Statement stmt) {
    return this.delegate.isExitStmt(stmt);
  }

  @Override
  public Collection<Statement> getEndPointsOf(Method m) {
    return this.delegate.getStartPointsOf(m);
  }

  @Override
  public boolean isCallStmt(Statement stmt) {
    return this.delegate.isCallStmt(stmt);
  }

  @Override
  public void addCalleeListener(CalleeListener listener) {
    delegate.addCalleeListener(listener);
  }

  @Override
  public void addCallerListener(CallerListener listener) {
    delegate.addCallerListener(listener);
  }

  @Override
  public int getNumberOfEdgesTakenFromPrecomputedGraph() {
    return delegate.getNumberOfEdgesTakenFromPrecomputedGraph();
  }

  @Override
  public void resetCallGraph() {
    delegate.resetCallGraph();
  }

  @Override
  public void computeFallback() {
    delegate.computeFallback();
  }

  @Override
  public void addEdges(Edge e) {
    this.delegate.addEdges(e);
  }
}
