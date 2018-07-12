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
package boomerang.seedfactory;

import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.*;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

import java.util.*;

/**
 * Created by johannesspath on 07.12.17.
 */
public abstract class SeedFactory<W extends Weight> {

    private final WeightedPushdownSystem<Method, INode<Reachable>, Weight.NoWeight> pds = new WeightedPushdownSystem<>();
    private final Multimap<Query,Transition<Method, INode<Reachable>>> seedToTransition = HashMultimap.create();

    private final WeightedPAutomaton<Method,  INode<Reachable>, Weight.NoWeight> automaton = new WeightedPAutomaton<Method,  INode<Reachable>, Weight.NoWeight>(wrap(Reachable.entry())) {
        @Override
        public INode<Reachable> createState(INode<Reachable> reachable, Method loc) {
            return new GeneratedState<>(reachable,loc);
        }

        @Override
        public boolean isGeneratedState(INode<Reachable> reachable) {
            return reachable instanceof GeneratedState;
        }


        @Override
        public Method epsilon() {
            return Method.epsilon();
        }

        @Override
        public Weight.NoWeight getZero() {
            return Weight.NO_WEIGHT_ZERO;
        }

        @Override
        public Weight.NoWeight getOne() {
            return Weight.NO_WEIGHT_ONE;
        }
    };
	private Collection<Method> processed = Sets.newHashSet();
	private Multimap<Query, SootMethod> queryToScope = HashMultimap.create();

    public Collection<Query> computeSeeds(){
        List<SootMethod> entryPoints = Scene.v().getEntryPoints();
        for(SootMethod m : entryPoints){
            automaton.addTransition(new Transition<>(wrap(Reachable.v()),new Method(m),automaton.getInitialState()));
        }
        pds.poststar(automaton);
        automaton.registerListener(new WPAUpdateListener<Method, INode<Reachable>, Weight.NoWeight>() {
            @Override
            public void onWeightAdded(Transition<Method, INode<Reachable>> t, Weight.NoWeight noWeight, WeightedPAutomaton<Method, INode<Reachable>, Weight.NoWeight> aut) {
                process(t);
            }
        });
        
        if(analyseClassInitializers()){
        	Set<SootClass> sootClasses = Sets.newHashSet();
        	for(Method p : Sets.newHashSet(processed)){
        		if(sootClasses.add(p.getMethod().getDeclaringClass())){
        			addStaticInitializerFor(p.getMethod().getDeclaringClass());
        		}
        	}
        }
        
        return seedToTransition.keySet();
    }

    private void addStaticInitializerFor(SootClass declaringClass) {
		for(SootMethod m : declaringClass.getMethods()){
			if(m.isStaticInitializer()){
		        for(SootMethod ep : Scene.v().getEntryPoints()){
		        	addPushRule(new Method(ep), new Method(m));
		        }
			}
		}
	}

	protected boolean analyseClassInitializers() {
		return false;
	}

	protected abstract Collection<? extends Query> generate(SootMethod method, Stmt u, Collection<SootMethod> calledMethods);
	
    private void process(Transition<Method, INode<Reachable>> t) {
        Method curr = t.getLabel();
    	if(!processed.add(curr))
    		return;
        SootMethod m = curr.getMethod();
    	if(!m.hasActiveBody())
    	    return;
    	for(Unit u : m.getActiveBody().getUnits()) {
            Collection<SootMethod> calledMethods = new HashSet<>();
			if (icfg().isCallStmt(u)) {
				icfg().addCalleeListener(new CalledMethodsCalleeListener(u, m, calledMethods));
			}
            for (Query seed : generate(m, (Stmt) u, calledMethods)) {
                seedToTransition.put(seed, t);
            }
            if (icfg().isCallStmt(u)) {
            	icfg().addCalleeListener(new AddPushRuleCalleeListener(u, m));
            }
        }
    }

    private class CalledMethodsCalleeListener implements CalleeListener<Unit,SootMethod>{
    	Unit u;
    	SootMethod m;
		Collection<SootMethod> calledMethods;

    	CalledMethodsCalleeListener(Unit u, SootMethod m, Collection<SootMethod> calledMethods){
    		this.u = u;
    		this.m = m;
    		this.calledMethods = calledMethods;
		}

		@Override
		public Unit getObservedCaller() {
			return u;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			calledMethods.add(sootMethod);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CalledMethodsCalleeListener that = (CalledMethodsCalleeListener) o;
			return Objects.equals(u, that.u) &&
					Objects.equals(m, that.m);
		}

		@Override
		public int hashCode() {

			return Objects.hash(u, m, CalledMethodsCalleeListener.class);
		}
	}

	private class AddPushRuleCalleeListener implements CalleeListener<Unit,SootMethod>{
		Unit u;
		SootMethod m;

		AddPushRuleCalleeListener(Unit u, SootMethod m){
			this.u = u;
			this.m = m;
		}

		@Override
		public Unit getObservedCaller() {
			return u;
		}

		@Override
		public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
			if (sootMethod.hasActiveBody()){
				addPushRule(new Method(m), new Method(sootMethod));
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AddPushRuleCalleeListener that = (AddPushRuleCalleeListener) o;
			return Objects.equals(u, that.u) &&
					Objects.equals(m, that.m);
		}

		@Override
		public int hashCode() {

			return Objects.hash(u, m, AddPushRuleCalleeListener.class);
		}
	}

    private void addPushRule(Method caller, Method callee) {
        pds.addRule(new PushRule<>(wrap(Reachable.v()),caller,wrap(Reachable.v()),callee,caller, Weight.NO_WEIGHT_ONE));
    }

    private INode<Reachable> wrap(Reachable r){
        return new SingleNode<>(r);
    }

    public abstract ObservableICFG<Unit,SootMethod> icfg();

	public Collection<SootMethod> getMethodScope(Query query) {
		Set<SootMethod> scope = Sets.newHashSet();
		if(queryToScope.containsKey(query)){
			return queryToScope.get(query);
		}
		for(Transition<Method, INode<Reachable>> t : seedToTransition.get(query)){
			scope.add(t.getLabel().getMethod());
			automaton.registerListener(new TransitiveClosure(t.getTarget(), scope, query));
		}
		queryToScope.putAll(query, scope);
		return scope;		
	}

	private class TransitiveClosure extends WPAStateListener<Method, INode<Reachable>, Weight.NoWeight>{

		private final Set<SootMethod> scope;
		private Query query;

		public TransitiveClosure(INode<Reachable> start, Set<SootMethod> scope, Query query) {
			super(start);
			this.scope = scope;
			this.query = query;
		}

		@Override
		public void onOutTransitionAdded(Transition<Method, INode<Reachable>> t, NoWeight w,
				WeightedPAutomaton<Method, INode<Reachable>, NoWeight> weightedPAutomaton) {
			scope.add(t.getLabel().getMethod());
			automaton.registerListener(new TransitiveClosure(t.getTarget(), scope, query));
		}

		@Override
		public void onInTransitionAdded(Transition<Method, INode<Reachable>> t, NoWeight w,
				WeightedPAutomaton<Method, INode<Reachable>, NoWeight> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((query == null) ? 0 : query.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			TransitiveClosure other = (TransitiveClosure) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (query == null) {
				if (other.query != null)
					return false;
			} else if (!query.equals(other.query))
				return false;
			return true;
		}

		private SeedFactory getOuterType() {
			return SeedFactory.this;
		}
	}

	public Collection<SootMethod> getAnyMethodScope() {
		Set<SootMethod> out = Sets.newHashSet();
		for(Query q : seedToTransition.keySet()) {
			out.addAll(getMethodScope(q));
		}
		return out;
	}


}
