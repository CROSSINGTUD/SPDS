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
package boomerang.weights;

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.WeightedBoomerang;
import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;

public abstract class PathTrackingBoomerang extends WeightedBoomerang<DataFlowPathWeight> {
	
	private OneWeightFunctions<Statement, Val, Field, DataFlowPathWeight> fieldWeights;
	private PathTrackingWeightFunctions callWeights;

	public PathTrackingBoomerang(){
		super();
	}
	public PathTrackingBoomerang(BoomerangOptions opt){
		super(opt);
	}
	
	@Override
	protected WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getForwardFieldWeights() {
		return getOrCreateFieldWeights();
	}

	
	@Override
	protected WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getBackwardFieldWeights() {
		return getOrCreateFieldWeights();
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> getBackwardCallWeights() {
		return getOrCreateCallWeights();
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
		return getOrCreateCallWeights();
	}

	@Override
	public Debugger<DataFlowPathWeight> createDebugger() {
		return new Debugger<>();
	}
	
	private WeightFunctions<Statement, Val, Field, DataFlowPathWeight> getOrCreateFieldWeights() {
		if(fieldWeights == null) {
			fieldWeights = new OneWeightFunctions<Statement, Val, Field, DataFlowPathWeight>(DataFlowPathWeight.zero(), DataFlowPathWeight.one());
		}
		return fieldWeights;
	}
	private WeightFunctions<Statement, Val, Statement, DataFlowPathWeight> getOrCreateCallWeights() {
		if(callWeights == null) {
			callWeights = new PathTrackingWeightFunctions();
		}
		return callWeights;
	}

}
