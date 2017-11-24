package dacapo;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import boomerang.ForwardQuery;
import boomerang.debugger.Debugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import ideal.IDEALAnalysis;
import ideal.IDEALAnalysisDefinition;
import ideal.ResultReporter;
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
import sync.pds.solver.WeightFunctions;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class IDEALRunner  extends ResearchQuestion  {

  protected IDEALAnalysis<TransitionFunction> createAnalysis() {
    String className = System.getProperty("rule");
    try {
    	final JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG(false);
    	System.out.println("Reachable Methods" +  Scene.v().getReachableMethods().size());
//    	Analysis.ALIASING_FOR_STATIC_FIELDS = false;
//    	Analysis.SEED_IN_APPLICATION_CLASS_METHOD = true;
//		AliasFinder.HANDLE_EXCEPTION_FLOW = false;
		final TypeStateMachineWeightFunctions genericsType = (TypeStateMachineWeightFunctions) Class.forName(className).getConstructor()
          .newInstance();
		
		return new IDEALAnalysis<TransitionFunction>(new IDEALAnalysisDefinition<TransitionFunction>() {

			@Override
			public Collection<Val> generate(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod) {
				return genericsType.generateSeed(method, stmt, calledMethod);
			}

			@Override
			public WeightFunctions<Statement, Val, Statement, TransitionFunction> weightFunctions() {
				return genericsType;
			}

			@Override
			public ResultReporter<TransitionFunction> resultReporter() {
				return new ResultReporter<TransitionFunction>() {

					@Override
					public void onSeedFinished(ForwardQuery seed,
							AbstractBoomerangSolver<TransitionFunction> seedSolver) {
						System.out.println("Seed finished " + seed);
					}
				};
			}

			@Override
			public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
				return icfg;
			}

			@Override
			public boolean enableAliasing() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public long analysisBudgetInSeconds() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean enableNullPointOfAlias() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean enableStrongUpdates() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Debugger<TransitionFunction> debugger() {
				return new Debugger<>();
			}
		}){};
    	
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  private IDEALAnalysis<TransitionFunction> analysis;
  protected long analysisTime;

  public void run() {
    G.v().reset();

    setupSoot();
    Transform transform = new Transform("wjtp.ifds", new SceneTransformer() {
      protected void internalTransform(String phaseName,
          @SuppressWarnings("rawtypes") Map options) {
        if (Scene.v().getMainMethod() == null)
          throw new RuntimeException("No main class existing.");
        for(SootClass c : Scene.v().getClasses()){
        	for(String app : IDEALRunner.this.getApplicationClasses()){
        		if(c.isApplicationClass())
        			continue;
        		if(c.toString().startsWith(app.replace("<",""))){
        			c.setApplicationClass();
        		}
        	}
        }
        System.out.println("Application Classes: " + Scene.v().getApplicationClasses().size());
        IDEALRunner.this.getAnalysis().run();
      }
    });

//    PackManager.v().getPack("wjtp").add(new Transform("wjtp.prep", new PreparationTransformer()));
    PackManager.v().getPack("wjtp").add(transform);
    PackManager.v().getPack("cg").apply();
    PackManager.v().getPack("wjtp").apply();
  }


  protected IDEALAnalysis<TransitionFunction> getAnalysis() {
    if (analysis == null)
      analysis = createAnalysis();
    return analysis;
  }
  
  protected long getBudget(){
	  return TimeUnit.SECONDS.toMillis(30);
  }
}
