package ideal;

import java.util.Collection;

import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import boomerang.WeightedForwardQuery;
import boomerang.debugger.Debugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.WeightFunctions;
import wpds.impl.Weight;

public abstract class IDEALAnalysisDefinition<W extends Weight> {

	/**
	 * This function generates the seed. Each (reachable) statement of the
	 * analyzed code is visited. To place a seed, a pair of access graph and an
	 * edge function must be specified. From this node the analysis starts its
	 * analysis.
	 * 
	 * @param method
	 * @param stmt
	 *            The statement over which is itearted over
	 * @param calledMethod
	 *            If stmt is a call site, this set contains the set of called
	 *            method for the call site.
	 * @return
	 */
	public abstract Collection<WeightedForwardQuery<W>> generate(SootMethod method, Unit stmt, Collection<SootMethod> calledMethod);

	/**
	 * This function must generate and return the AnalysisEdgeFunctions that are
	 * used for the analysis. As for standard IDE in Heros, the edge functions
	 * for normal-, call-, return- and call-to-return flows have to be
	 * specified.
	 */
	public abstract WeightFunctions<Statement,Val,Statement,W> weightFunctions();

	public abstract BiDiInterproceduralCFG<Unit, SootMethod> icfg();

	public abstract long analysisBudgetInSeconds();

	public abstract boolean enableStrongUpdates();

	public String toString() {
		String str = "====== IDEal Analysis Options ======";
//		str += "\nEdge Functions:\t\t" + edgeFunctions();
//		str += "\nDebugger Class:\t\t" + debugger();
//		str += "\nAnalysisBudget(sec):\t" + (analysisBudgetInSeconds());
//		str += "\nStrong Updates:\t\t" + (enableStrongUpdates() ? "ENABLED" : "DISABLED");
//		str += "\nAliasing:\t\t" + (enableAliasing() ? "ENABLED" : "DISABLED");
//		str += "\nNull POAs:\t\t" + (enableNullPointOfAlias() ? "ENABLED" : "DISABLED");
//		str += "\n" + boomerangOptions();
		return str;
	}

	public abstract Debugger<W> debugger();

	public BoomerangOptions boomerangOptions() {
		return new DefaultBoomerangOptions();
	}
}
