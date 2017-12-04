package dacapo;
import java.util.Map;

import boomerang.ForwardQuery;
import boomerang.WholeProgramBoomerang;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import wpds.impl.Weight.NoWeight;

public class DacapoRunner  extends ResearchQuestion  {

  protected long analysisTime;
  public static void main(String...args) {
	System.setProperty("benchmarkFolder", args[0]);
	System.setProperty("benchmark", args[1]);

	  new DacapoRunner().run();
  }
  public void run() {
    G.v().reset();

    setupSoot();
    Transform transform = new Transform("wjtp.ifds", new SceneTransformer() {
      protected void internalTransform(String phaseName,
          @SuppressWarnings("rawtypes") Map options) {
        if (Scene.v().getMainMethod() == null)
          throw new RuntimeException("No main class existing.");
        for(SootClass c : Scene.v().getClasses()){
        	for(String app : DacapoRunner.this.getApplicationClasses()){
        		if(c.isApplicationClass())
        			continue;
        		if(c.toString().startsWith(app.replace("<",""))){
        			c.setApplicationClass();
        		}
        	}
        }

    	final JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
    	WholeProgramBoomerang<NoWeight> solver = new WholeProgramBoomerang<NoWeight>() {
			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return icfg;
			}
			
			@Override
			public Debugger createDebugger() {
				return new Debugger<>();
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, NoWeight> getForwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Field, NoWeight> getBackwardFieldWeights() {
				return new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, NoWeight> getBackwardCallWeights() {
				return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
			}

			@Override
			protected WeightFunctions<Statement, Val, Statement, NoWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
				return new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
			}

		};
		solver.wholeProgramAnalysis();
        System.out.println("Application Classes: " + Scene.v().getApplicationClasses().size());
      }
    });

    PackManager.v().getPack("wjtp").add(transform);
    PackManager.v().getPack("cg").apply();
    PackManager.v().getPack("wjtp").apply();
  }


}
