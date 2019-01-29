package reachability;

import java.util.Map;
import java.util.Set;

import boomerang.callgraph.ObservableDynamicICFG;
import boomerang.preanalysis.BoomerangPretransformer;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import typestate.dacapo.SootSceneSetupDacapo;

public class DacapoReachabilityAnalysis extends SootSceneSetupDacapo {

	public DacapoReachabilityAnalysis(String benchmarkFolder, String project) {
		super(benchmarkFolder, project);
	}

	public void run() {
		G.v().reset();
		setupSoot();
		Transform transform = new Transform("wjtp.ifds", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				BoomerangPretransformer.v().apply();
				ObservableDynamicICFG observableDynamicICFG = new ObservableDynamicICFG(false);
				ReachabilityAnalysis reachabilityAnalysis = new ReachabilityAnalysis(observableDynamicICFG);
				Set<SootMethod> reachbleFrom = reachabilityAnalysis.reachbleFrom(Scene.v().getEntryPoints());
				System.out.println("Computed " + reachbleFrom.size() + " reachable methods");

			}
		});

		// PackManager.v().getPack("wjtp").add(new Transform("wjtp.prep", new
		// PreparationTransformer()));
		PackManager.v().getPack("wjtp").add(transform);
		PackManager.v().getPack("cg").apply();
		PackManager.v().getPack("wjtp").apply();
	}
}
