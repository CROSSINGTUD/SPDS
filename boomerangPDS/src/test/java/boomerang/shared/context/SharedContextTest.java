package boomerang.shared.context;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.shared.context.targets.BasicTarget;
import boomerang.shared.context.targets.BranchingAfterNewStringTest;
import boomerang.shared.context.targets.BranchingTest;
import boomerang.shared.context.targets.ContextSensitiveAndLeftUnbalancedTarget;
import boomerang.shared.context.targets.ContextSensitiveTarget;
import boomerang.shared.context.targets.LeftUnbalancedTarget;
import boomerang.shared.context.targets.NestedContextAndBranchingTarget;
import boomerang.shared.context.targets.NestedContextTarget;
import boomerang.shared.context.targets.WrappedInNewStringInnerTarget;
import boomerang.shared.context.targets.WrappedInNewStringTarget;
import boomerang.shared.context.targets.WrappedInStringTwiceTest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public class SharedContextTest {

  public static String CG = "cha";

  @Test
  public void basicTarget() {
    setupSoot(BasicTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.BasicTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void leftUnbalancedTargetTest() {
    setupSoot(LeftUnbalancedTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.LeftUnbalancedTarget: void bar(java.lang.String)>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveTest() {
    setupSoot(ContextSensitiveTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.ContextSensitiveTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }


  @Test
  public void nestedContextTest() {
    setupSoot(NestedContextTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.NestedContextTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }


  @Test
  public void nestedContextAndBranchingTest() {
    setupSoot(NestedContextAndBranchingTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.NestedContextAndBranchingTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedTest() {
    setupSoot(ContextSensitiveAndLeftUnbalancedTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.ContextSensitiveAndLeftUnbalancedTarget: void context(java.lang.String)>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void wrappedInNewStringTest() {
    setupSoot(WrappedInNewStringTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.WrappedInNewStringTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void wrappedInNewStringInnerTest() {
    setupSoot(WrappedInNewStringInnerTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.WrappedInNewStringInnerTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }


  @Test
  public void wrappedInNewStringTwiceTest() {
    setupSoot(WrappedInStringTwiceTest.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.WrappedInStringTwiceTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void branchingTest() {
    setupSoot(BranchingTest.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.BranchingTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  @Test
  public void branchingAfterNewTest() {
    setupSoot(BranchingAfterNewStringTest.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.shared.context.targets.BranchingAfterNewStringTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  public static BackwardQuery selectFirstFileInitArgument(SootMethod m) {
    Method method = JimpleMethod.of(m);
    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement =
        method.getStatements().stream()
            .filter(x -> x.containsInvokeExpr())
            .filter(
                x -> {
                  x.toString();
                  return true;
                })
            .filter(
                x ->
                    x.getInvokeExpr().getMethod().getName().equals("<init>")
                        && x.getInvokeExpr()
                            .getMethod()
                            .getDeclaringClass()
                            .getFullyQualifiedName()
                            .equals("java.io.File"))
            .findFirst()
            .get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  protected void runAnalysis(BackwardQuery query, String... expectedValues) {
    // TODO move to analysis
    Specification specification = Specification
        .create("<GO{F}java.lang.String: void <init>(ON{F}java.lang.String)>","<ON{B}java.lang.String: void <init>(GO{B}java.lang.String)>)");
    SharedContextAnalysis sharedContextAnalysis = new SharedContextAnalysis(specification);
    Collection<ForwardQuery> res = sharedContextAnalysis.run(query);
    Assert.assertEquals(
        Sets.newHashSet(expectedValues),
        res.stream()
            .map(t -> ((AllocVal) t.var()).getAllocVal())
            .filter(x -> x.isStringConstant())
            .map(x -> x.getStringValue())
            .collect(Collectors.toSet()));
  }

  protected void setupSoot(Class cls) {
    G.v().reset();
    setupSoot();
    setApplicationClass(cls);
    PackManager.v().runPacks();
    BoomerangPretransformer.v().reset();
    BoomerangPretransformer.v().apply();
  }

  private void setupSoot() {
    Options.v().set_whole_program(true);
    Options.v().setPhaseOption("cg." + CG, "on");
    Options.v().setPhaseOption("cg." + CG, "verbose:true");
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_keep_line_number(true);
    Options.v().set_prepend_classpath(true);
    Options.v().set_process_dir(getProcessDir());
  }

  private void setApplicationClass(Class cls) {
    Scene.v().loadNecessaryClasses();
    List<SootMethod> eps = Lists.newArrayList();
    for (SootClass sootClass : Scene.v().getClasses()) {
      if (sootClass.toString().equals(cls.getName())
          || (sootClass.toString().contains(cls.getName() + "$"))) {
        sootClass.setApplicationClass();
        eps.addAll(sootClass.getMethods());
      }
    }
    Scene.v().setEntryPoints(eps);
  }

  private List<String> getProcessDir() {
    Path path = Paths.get("target/test-classes");
    return Lists.newArrayList(path.toAbsolutePath().toString());
  }
}
