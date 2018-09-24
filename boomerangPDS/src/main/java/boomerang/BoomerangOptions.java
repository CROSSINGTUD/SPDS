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
package boomerang;

import com.google.common.base.Optional;

import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import boomerang.stats.IBoomerangStats;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;

public interface BoomerangOptions {
	
	public boolean staticFlows();
	
	public boolean arrayFlows();
	public boolean fastForwardFlows();
	public boolean typeCheck();
	public boolean onTheFlyCallGraph();
	public boolean throwFlows();
	
	public boolean callSummaries();
	public boolean fieldSummaries();
	
	public int analysisTimeoutMS();

	public boolean isAllocationVal(Value val);

	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, ObservableICFG<Unit, SootMethod> icfg);

	public boolean isIgnoredMethod(SootMethod method);
	public IBoomerangStats statsFactory();

	public boolean aliasing();
	
	
	/**
	 * Assume we propagate an object of soot.NullType in variable y and the propagation reaches a statement x = (Object) y.
	 * @return If set to true, the propagation will NOT continue in x. This does not match the runtime semantics.   
	 * At runtime, null can be cast to any RefType! Though a check (null instanceof Object) returns false.
	 */
	public boolean killNullAtCast();
}
