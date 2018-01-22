/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang;

import boomerang.stats.IBoomerangStats;
import com.google.common.base.Optional;

import boomerang.jimple.AllocVal;
import boomerang.jimple.Val;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

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

	public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg);

	public boolean isIgnoredMethod(SootMethod method);
	public IBoomerangStats statsFactory();
}
