package test.slowmethod;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import boomerang.BoomerangOptions;
import boomerang.accessgraph.AccessGraph;
import boomerang.cfg.IExtendedICFG;
import boomerang.ifdssolver.IPathEdge;
import boomerang.ifdssolver.IPropagationController;
import ideal.Analysis;
import ideal.ResultReporter;
import ideal.debug.IDebugger;
import soot.SootMethod;
import soot.Unit;
import test.IDEALTestingFramework;
import typestate.ConcreteState;
import typestate.TypestateAnalysisProblem;
import typestate.TypestateChangeFunction;
import typestate.TypestateDomainValue;

public abstract class SlowMethodDetector extends IDEALTestingFramework {

	private LinkedHashMap<SootMethod, Long> addedMethodToAnalysisTime = new LinkedHashMap<>();
	Set<SootMethod> visitableMethod = new HashSet<>();
	LinkedList<SootMethod> worklist = new LinkedList<>();

	@Override
	protected void executeAnalysis() {
		worklist.add(sootTestMethod);
		long lastIterationTime = 0;
		while(!worklist.isEmpty()){
			SootMethod poll = worklist.poll();
			Stopwatch watch = Stopwatch.createStarted();
			if(!visitableMethod.add(poll))
				continue;
			super.executeAnalysis();
			long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);
			addedMethodToAnalysisTime.put(poll, elapsed);
			if(lastIterationTime != 0){
				float factor = ((float) elapsed)/((float) lastIterationTime);
				if(factor > 1.5){
					System.err.println("Method that slows down analysis: ");
					System.err.println("\t\t" + poll);
					System.err.println(lastIterationTime + "\t\t" + elapsed);
				}
			}
			lastIterationTime = elapsed;
		}
		System.out.println("Analysis run \t\t Analysis Time (sec) \t\t Methods");
		int i = 0;
		for (Entry<SootMethod, Long> e : addedMethodToAnalysisTime.entrySet()) {
			System.out.println("" + i + " \t\t" + e.getValue() + "\t\t" + e.getKey());
			i++;
		}
	}

	private boolean isWithinAllowedMethod(Unit n) {
		boolean contains = visitableMethod.contains(icfg.getMethodOf(n));
		if (!contains) {
			worklist.add(icfg.getMethodOf(n));
		}
		return contains;
	}

	protected Analysis<TypestateDomainValue<ConcreteState>> createAnalysis() {
		return new Analysis<TypestateDomainValue<ConcreteState>>(new TypestateAnalysisProblem<ConcreteState>() {
			@Override
			public ResultReporter<TypestateDomainValue<ConcreteState>> resultReporter() {
				return SlowMethodDetector.this.testingResultReporter;
			}

			@Override
			public IExtendedICFG icfg() {
				return SlowMethodDetector.this.icfg;
			}

			@Override
			public IDebugger<TypestateDomainValue<ConcreteState>> debugger() {
				return SlowMethodDetector.this.getDebugger();
			}

			@Override
			public TypestateChangeFunction<ConcreteState> createTypestateChangeFunction() {
				return SlowMethodDetector.this.createTypestateChangeFunction();
			}

			@Override
			public heros.solver.IPropagationController<Unit, AccessGraph> propagationController() {
				return new heros.solver.IPropagationController<Unit, AccessGraph>() {

					@Override
					public boolean continuePropagate(AccessGraph d1, Unit n, AccessGraph d2) {
						return SlowMethodDetector.this.isWithinAllowedMethod(n);
					}
				};
			}

			@Override
			public BoomerangOptions boomerangOptions() {
				return new BoomerangOptions() {
					@Override
					public IPropagationController<Unit, AccessGraph> propagationController() {
						return new IPropagationController<Unit, AccessGraph>() {

							@Override
							public boolean continuePropagate(IPathEdge<Unit, AccessGraph> edge) {
								return SlowMethodDetector.this.isWithinAllowedMethod(edge.getTarget());
							}
						};
					}
					@Override
					public IExtendedICFG icfg() {
						return SlowMethodDetector.this.icfg;
					}
					@Override
					public long getTimeBudget() {
						return 500;
					}
					@Override
					public boolean getTrackStaticFields() {
						return Analysis.ALIASING_FOR_STATIC_FIELDS;
					}
					
				};
			}
			@Override
			public String toString() {
				return "SlowMethodDebuggerOptions";
			}
		});
	}
}
