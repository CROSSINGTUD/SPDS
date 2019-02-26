/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package ideal;

import boomerang.BoomerangOptions;
import boomerang.DefaultBoomerangOptions;
import boomerang.WeightedForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.debugger.Debugger;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import sync.pds.solver.WeightFunctions;
import wpds.impl.Weight;

import java.util.Collection;

public abstract class IDEALAnalysisDefinition<W extends Weight> {

	protected ObservableICFG<Unit, SootMethod> icfg;

	/**
	 * This function generates the seed. Each (reachable) statement of the
	 * analyzed code is visited. To place a seed, a pair of access graph and an
	 * edge function must be specified. From this node the analysis starts its
	 * analysis.
	 * 
	 * @param method
	 * @param stmt
	 *            The statement over which is iterated over
	 * @return
	 */
	public abstract Collection<WeightedForwardQuery<W>> generate(SootMethod method, Unit stmt);

	/**
	 * This function must generate and return the AnalysisEdgeFunctions that are
	 * used for the analysis. As for standard IDE in Heros, the edge functions
	 * for normal-, call-, return- and call-to-return flows have to be
	 * specified.
	 */
	public abstract WeightFunctions<Statement,Val,Statement,W> weightFunctions();

	public ObservableICFG<Unit, SootMethod> icfg(){
		return icfg;
	}

	public boolean enableStrongUpdates() {
		return true;
	}

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

	public abstract Debugger<W> debugger(IDEALSeedSolver<W> idealSeedSolver);

	public BoomerangOptions boomerangOptions() {
		return new DefaultBoomerangOptions();
	}
	
	
	public IDEALResultHandler getResultHandler(){
		return new IDEALResultHandler();
	};
}
