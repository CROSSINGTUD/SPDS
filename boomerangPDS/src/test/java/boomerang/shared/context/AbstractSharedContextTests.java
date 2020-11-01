package boomerang.shared.context;


import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.results.AffectedLocation;
import boomerang.results.PathElement;
import boomerang.results.QueryResults;
import boomerang.scene.CallGraph;
import boomerang.scene.DataFlowScope;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;

public abstract class AbstractSharedContextTests {
  public static String CG = "cha";
  private static Logger LOGGER = LoggerFactory.getLogger(AbstractSharedContextTests.class);

  protected void setupSoot(Class cls){
    G.v().reset();
    setupSoot();
    setApplicationClass(cls);
    PackManager.v().runPacks();
    BoomerangPretransformer.v().reset();
    BoomerangPretransformer.v().apply();
  }
  protected void runAnalysis(Class cls, BackwardQuery query, String... expectedValues) {
    //TODO move to analysis
    SharedContextAnalysis sharedContextAnalysis = new SharedContextAnalysis();
    Collection<ForwardQuery> res = sharedContextAnalysis.run(query);
    System.out.println(res);
    Assert.assertTrue(res.stream().filter(x -> x.var().isStringConstant() && x.var().getStringValue().equals(expectedValues)).count() > 0);
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

  private List<String> getProcessDir() {
    Path path = Paths.get("target/test-classes");
    return Lists.newArrayList(path.toAbsolutePath().toString());
  }
}
