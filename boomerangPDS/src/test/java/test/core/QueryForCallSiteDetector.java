package test.core;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.scene.AnalysisScope;
import boomerang.scene.CallGraph;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import boomerang.scene.jimple.AccessPathParser;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.util.AccessPath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class QueryForCallSiteDetector extends AnalysisScope {

  boolean resultsMustNotBeEmpty = false;
  boolean accessPathQuery = false;
  boolean integerQueries;
  Set<AccessPath> expectedAccessPaths = new HashSet<>();

  QueryForCallSiteDetector(CallGraph cg) {
    super(cg);
  }

  private void getAllExpectedAccessPath(Edge u) {
    Val arg = u.getStart().getInvokeExpr().getArg(1);
    if (arg.isStringConstant()) {
      String value = arg.getStringValue();
      expectedAccessPaths.addAll(
          AccessPathParser.parseAllFromString(value, (JimpleMethod) u.getMethod()));
    }
  }

  private class FirstArgumentOf implements ValueOfInterestInUnit {

    private String methodNameMatcher;

    public FirstArgumentOf(String methodNameMatcher) {
      this.methodNameMatcher = methodNameMatcher;
    }

    @Override
    public Optional<? extends Query> test(Edge stmt) {
      if (!(stmt.getTarget().containsInvokeExpr())) return Optional.empty();
      boomerang.scene.InvokeExpr invokeExpr = stmt.getTarget().getInvokeExpr();
      if (!invokeExpr.getMethod().getName().matches(methodNameMatcher)) return Optional.empty();
      Val param = invokeExpr.getArg(0);
      if (!param.isLocal()) return Optional.empty();
      BackwardQuery newBackwardQuery = BackwardQuery.make(stmt, param);
      return Optional.<Query>of(newBackwardQuery);
    }
  }

  @Override
  protected Collection<? extends Query> generate(Edge stmt) {
    Optional<? extends Query> query = new FirstArgumentOf("queryFor").test(stmt);

    if (query.isPresent()) {
      return Collections.singleton(query.get());
    }
    query = new FirstArgumentOf("queryForAndNotEmpty").test(stmt);

    if (query.isPresent()) {
      resultsMustNotBeEmpty = true;
      return Collections.singleton(query.get());
    }
    query = new FirstArgumentOf("intQueryFor").test(stmt);
    if (query.isPresent()) {
      integerQueries = true;
      return Collections.singleton(query.get());
    }

    query = new FirstArgumentOf("accessPathQueryFor").test(stmt);
    if (query.isPresent()) {
      accessPathQuery = true;
      getAllExpectedAccessPath(stmt);
      return Collections.singleton(query.get());
    }
    return Collections.emptySet();
  }
}
