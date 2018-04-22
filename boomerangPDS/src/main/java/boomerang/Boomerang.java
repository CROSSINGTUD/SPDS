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

import boomerang.debugger.Debugger;
import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;
import wpds.impl.Weight.NoWeight;

public abstract class Boomerang extends WeightedBoomerang<NoWeight> {
	
	private OneWeightFunctions<Statement, Val, Field, NoWeight> fieldWeights;
	private OneWeightFunctions<Statement, Val, Statement, NoWeight> callWeights;

	public Boomerang(){
		super();
	}
	public Boomerang(BoomerangOptions opt){
		super(opt);
	}
	
	@Override
	protected WeightFunctions<Statement, Val, Field, NoWeight> getForwardFieldWeights() {
		return getOrCreateFieldWeights();
	}

	
	@Override
	protected WeightFunctions<Statement, Val, Field, NoWeight> getBackwardFieldWeights() {
		return getOrCreateFieldWeights();
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, NoWeight> getBackwardCallWeights() {
		return getOrCreateCallWeights();
	}

	@Override
	protected WeightFunctions<Statement, Val, Statement, NoWeight> getForwardCallWeights(ForwardQuery sourceQuery) {
		return getOrCreateCallWeights();
	}

	@Override
	public Debugger<NoWeight> createDebugger() {
		return new Debugger<>();
	}
	
	private WeightFunctions<Statement, Val, Field, NoWeight> getOrCreateFieldWeights() {
		if(fieldWeights == null) {
			fieldWeights = new OneWeightFunctions<Statement, Val, Field, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
		}
		return fieldWeights;
	}
	private WeightFunctions<Statement, Val, Statement, NoWeight> getOrCreateCallWeights() {
		if(callWeights == null) {
			callWeights = new OneWeightFunctions<Statement, Val, Statement, NoWeight>(NoWeight.NO_WEIGHT_ZERO, NoWeight.NO_WEIGHT_ONE);
		}
		return callWeights;
	}

}
