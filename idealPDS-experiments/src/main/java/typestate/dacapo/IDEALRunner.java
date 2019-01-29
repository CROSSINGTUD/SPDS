package typestate.dacapo;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import boomerang.WeightedForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.debugger.Debugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.preanalysis.BoomerangPretransformer;
import boomerang.results.ForwardBoomerangResults;
import experiments.google.spreadsheet.GoogleSpreadsheetWriter;
import ideal.IDEALAnalysis;
import ideal.IDEALAnalysisDefinition;
import ideal.IDEALResultHandler;
import ideal.IDEALSeedSolver;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.WeightFunctions;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class IDEALRunner extends SootSceneSetupDacapo  {

  public IDEALRunner(String benchmarkFolder, String project) {
		super(benchmarkFolder, project);
	}
	private Collection<WeightedForwardQuery<TransitionFunction>> printedSeeds = Sets.newHashSet();

protected IDEALAnalysis<TransitionFunction> createAnalysis() {
    String className = System.getProperty("rule");
    try {
		BoomerangPretransformer.v().reset();
		BoomerangPretransformer.v().apply();
    	System.out.println("Reachable Methods" +  Scene.v().getReachableMethods().size());
		final TypeStateMachineWeightFunctions genericsType = (TypeStateMachineWeightFunctions) Class.forName(className).getConstructor()
          .newInstance();
		
		return new IDEALAnalysis<TransitionFunction>(new IDEALAnalysisDefinition<TransitionFunction>() {
			

			@Override
			public Collection<WeightedForwardQuery<TransitionFunction>> generate(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod) {
				if(!method.getDeclaringClass().isApplicationClass())
					return Collections.emptyList();
				return genericsType.generateSeed(method, stmt, calledMethod);
			}

			@Override
			public WeightFunctions<Statement, Val, Statement, TransitionFunction> weightFunctions() {
				return genericsType;
			}
			
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				return icfg;
			}

			@Override
			public BoomerangOptions boomerangOptions() {
				return new DefaultBoomerangOptions() {
					@Override
					public int analysisTimeoutMS() {
						return (int) IDEALRunner.this.getBudget();
					}
					@Override
					public boolean arrayFlows() {
						return false;
					}
					
					@Override
					public boolean staticFlows() {
						return false;
					}
					
				};
			}
			@Override
			public Debugger<TransitionFunction> debugger(IDEALSeedSolver<TransitionFunction> solver) {
				return new Debugger<>();
//				File file = new File("idealDebugger/" + solver.getSeed());
//				file.getParentFile().mkdirs();
//				return new IDEVizDebugger<>(new File("idealDebugger/" + solver.getSeed()), icfg);
			}
			
			@Override
			public IDEALResultHandler<TransitionFunction> getResultHandler() {
				return new IDEALResultHandler<TransitionFunction>() {

					@Override
					public void report(WeightedForwardQuery<TransitionFunction> seed,
							ForwardBoomerangResults<TransitionFunction> res) {
						if(!printedSeeds.add(seed))
							return;
						
						try {
							GoogleSpreadsheetWriter.write(asCSVLine(seed, res));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (GeneralSecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						super.report(seed, res);
					}
				};
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
private String outputFile;

  public void run(final String outputFile) {
    G.v().reset();
    this.outputFile = outputFile;
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

    private List<Object> asCSVLine(WeightedForwardQuery<TransitionFunction> key, ForwardBoomerangResults<TransitionFunction> forwardBoomerangResults) {
    		//("Analysis;Rule;Seed;SeedStatement;SeedMethod;SeedClass;Is_In_Error;Timedout;AnalysisTimes;PropagationCount;VisitedMethod;ReachableMethods;CallRecursion;FieldLoop;MaxAccessPath\n");
    		String analysis = "ideal";
    		String rule = System.getProperty("ruleIdentifier");
    		String program = System.getProperty("program");
    		Stmt seedStmt = key.stmt().getUnit().get();
    		SootMethod seedMethod = key.stmt().getMethod();
    		SootClass seedClass = seedMethod.getDeclaringClass();
    		String isInErrorState = String.valueOf(isInErrorState(key,forwardBoomerangResults));
    		String isTimedout = String.valueOf(getAnalysis().isTimedout(key));
    		long elapsed = getAnalysis().getAnalysisTime(key).elapsed(TimeUnit.MILLISECONDS);
    		String analysisTime =  String.valueOf(elapsed < 0 ? 1 : elapsed);
    		String propagationCount =  String.valueOf(forwardBoomerangResults.getStats().getForwardReachesNodes().size());
    		String visitedMethods =  String.valueOf(forwardBoomerangResults.getStats().getCallVisitedMethods().size());
    		String reachableMethods =  String.valueOf(Scene.v().getReachableMethods().size());
    		String containsCallLoop =  String.valueOf(forwardBoomerangResults.containsCallRecursion());
    		String containsFieldLoop =  String.valueOf(forwardBoomerangResults.containsFieldLoop());
    		String usedMemory =  String.valueOf(forwardBoomerangResults.getMaxMemory());
        return Arrays.asList(new String[] {analysis,program,rule,key.toString(),seedStmt.toString(),seedMethod.toString(),seedClass.toString(),isInErrorState,isTimedout,analysisTime,propagationCount,visitedMethods,reachableMethods,containsCallLoop,containsFieldLoop, usedMemory});
    }

    private boolean isInErrorState(WeightedForwardQuery<TransitionFunction> key, ForwardBoomerangResults<TransitionFunction> forwardBoomerangResults) {
        Table<Statement, Val, TransitionFunction> objectDestructingStatements = forwardBoomerangResults.asStatementValWeightTable();
        for(Table.Cell<Statement,Val,TransitionFunction> c : objectDestructingStatements.cellSet()){
            for(ITransition t : c.getValue().values()){
                if(t.to() != null){
                    if(t.to().isErrorState()){
                        return true;
                    }
                }
            }

        }
      return false;
    }


    protected IDEALAnalysis<TransitionFunction> getAnalysis() {
    if (analysis == null)
      analysis = createAnalysis();
    return analysis;
  }
  
  protected long getBudget(){
	  return TimeUnit.MINUTES.toMillis(1);
  }
}
