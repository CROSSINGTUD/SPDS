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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import ideal.IDEALSeedSolver.Phases;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.NormalRule;
import wpds.impl.Rule;
import wpds.impl.Weight;

public class IDEALWeightFunctions<W extends Weight> implements WeightFunctions<Statement,Val,Statement,W> {

	private static final Logger logger = LogManager.getLogger();
	private WeightFunctions<Statement,Val,Statement,W> delegate;
	private Set<NonOneFlowListener<W>> listeners = Sets.newHashSet(); 
	private Set<Statement> potentialStrongUpdates = Sets.newHashSet();
	private Set<Statement> weakUpdates = Sets.newHashSet();
	private Multimap<Node<Statement,Val>, W> nonOneFlowNodes = HashMultimap.create();
	private Phases phase;
	private boolean strongUpdates;
	private Multimap<Node<Statement,Val>, Node<Statement,Val>> indirectAlias = HashMultimap.create(); 
	private Set<Node<Statement,Val>> nodesWithStrongUpdate = Sets.newHashSet();

	public IDEALWeightFunctions(WeightFunctions<Statement,Val,Statement,W>  delegate, boolean strongUpdates) {
		this.delegate = delegate;
		this.strongUpdates = strongUpdates;
	}
	
	@Override
	public W push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
		W weight = delegate.push(curr, succ, calleeSp);
		if (isObjectFlowPhase() &&!weight.equals(getOne())){	
			addOtherThanOneWeight(curr, weight);
		}
		return weight;
	}
	
	void addOtherThanOneWeight(Node<Statement, Val> curr, W weight) {
		if(nonOneFlowNodes.put(curr, weight)){
			for(NonOneFlowListener<W> l : Lists.newArrayList(listeners)){
				l.nonOneFlow(curr,weight);
			}
		}
	}

	@Override
	public W normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		W weight = delegate.normal(curr, succ);
		if (isObjectFlowPhase() && curr.stmt().isCallsite() && !weight.equals(getOne())){
			addOtherThanOneWeight(curr, weight);
		}
		return weight;
	}

	private boolean isObjectFlowPhase() {
		return phase.equals(Phases.ObjectFlow);
	}

	private boolean isValueFlowPhase() {
		return phase.equals(Phases.ValueFlow);
	}
	
	@Override
	public W pop(Node<Statement, Val> curr, Statement location) {
		return delegate.pop(curr, location);
	}

	public void registerListener(NonOneFlowListener<W> listener){
		if(listeners.add(listener)){
			for(Entry<Node<Statement, Val>, W> existing : Lists.newArrayList(nonOneFlowNodes.entries())){
				listener.nonOneFlow(existing.getKey(),existing.getValue());
			}
		}
	}
	
	
	@Override
	public W getOne() {
		return delegate.getOne();
	}

	@Override
	public W getZero() {
		return delegate.getZero();
	}


	@Override
	public String toString() {
		return "[IDEAL-Wrapped Weights] " + delegate.toString();
	}

	public void potentialStrongUpdate(Statement stmt) {
		potentialStrongUpdates.add(stmt);
	}
	
	public void weakUpdate(Statement stmt) {
		weakUpdates.add(stmt);
	}

	public void setPhase(Phases phase) {
		this.phase = phase;
	}

	public void addIndirectFlow(Node<Statement, Val> source, Node<Statement, Val> target) {
		logger.trace("Alias flow detected "+  source+ " " + target);
		indirectAlias.put(source, target);
	}

	public Collection<Node<Statement, Val>> getAliasesFor(Node<Statement, Val> node) {
		return indirectAlias.get(node);
	}
	
	public boolean isStrongUpdateStatement(Statement stmt) {
		return potentialStrongUpdates.contains(stmt) && !weakUpdates.contains(stmt) && strongUpdates;
	}

	public boolean isKillFlow(Node<Statement, Val> node) {
		return !nodesWithStrongUpdate.contains(node) && !indirectAlias.containsValue(node);
	}

	public void addNonKillFlow(Node<Statement, Val> curr) {
		nodesWithStrongUpdate.add(curr);
	}
}
