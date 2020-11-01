package boomerang.shared.context;

import boomerang.BackwardQuery;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleMethod;
import org.junit.Test;
import soot.Scene;
import soot.SootMethod;

public class SharedContextTest extends AbstractSharedContextTests {

  @Test
  public void simpleTest() {
    setupSoot(boomerang.shared.context.targets.SharedContextTarget1.class);
    SootMethod m = Scene.v().getMethod("<boomerang.shared.context.targets.SharedContextTarget1: void bar(java.lang.String)>");
    Method method = JimpleMethod.of(m);

    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement = method.getStatements().stream().filter(x -> x.containsInvokeExpr()).filter(x -> {x.toString(); return true;}).filter(
        x ->  x.getInvokeExpr().getMethod().getName().equals("<init>") && x.getInvokeExpr().getMethod().getDeclaringClass().getFullyQualifiedName().equals("java.io.File")).findFirst().get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor = method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    BackwardQuery query = BackwardQuery.make(cfgEdge, arg);

    runAnalysis(boomerang.shared.context.targets.SharedContextTarget1.class, query, "bar" );
  }

  @Test
  public void contextTest() {
    setupSoot(boomerang.shared.context.targets.SharedContextTarget2.class);
    SootMethod m = Scene.v().getMethod("<boomerang.shared.context.targets.SharedContextTarget2: void main(java.lang.String[])>");
    Method method = JimpleMethod.of(m);

    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement = method.getStatements().stream().filter(x -> x.containsInvokeExpr()).filter(x -> {x.toString(); return true;}).filter(
        x ->  x.getInvokeExpr().getMethod().getName().equals("<init>") && x.getInvokeExpr().getMethod().getDeclaringClass().getFullyQualifiedName().equals("java.io.File")).findFirst().get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor = method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    BackwardQuery query = BackwardQuery.make(cfgEdge, arg);

    runAnalysis(boomerang.shared.context.targets.SharedContextTarget1.class, query, "bar" );
  }


  @Test
  public void contextTestUnbalanced() {
    setupSoot(boomerang.shared.context.targets.SharedContextTarget3.class);
    SootMethod m = Scene.v().getMethod("<boomerang.shared.context.targets.SharedContextTarget3: void context(java.lang.String)>");
    Method method = JimpleMethod.of(m);

    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement = method.getStatements().stream().filter(x -> x.containsInvokeExpr()).filter(x -> {x.toString(); return true;}).filter(
        x ->  x.getInvokeExpr().getMethod().getName().equals("<init>") && x.getInvokeExpr().getMethod().getDeclaringClass().getFullyQualifiedName().equals("java.io.File")).findFirst().get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor = method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    BackwardQuery query = BackwardQuery.make(cfgEdge, arg);

    runAnalysis(boomerang.shared.context.targets.SharedContextTarget1.class, query, "bar" );
  }


  @Test
  public void contextTestUnbalancedSimple() {
    setupSoot(boomerang.shared.context.targets.SharedContextTarget3.class);
    SootMethod m = Scene.v().getMethod("<boomerang.shared.context.targets.SharedContextTarget4: void main(java.lang.String[])>");
    Method method = JimpleMethod.of(m);

    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement = method.getStatements().stream().filter(x -> x.containsInvokeExpr()).filter(x -> {x.toString(); return true;}).filter(
        x ->  x.getInvokeExpr().getMethod().getName().equals("<init>") && x.getInvokeExpr().getMethod().getDeclaringClass().getFullyQualifiedName().equals("java.io.File")).findFirst().get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor = method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    BackwardQuery query = BackwardQuery.make(cfgEdge, arg);

    runAnalysis(boomerang.shared.context.targets.SharedContextTarget1.class, query, "bar" );
  }
}