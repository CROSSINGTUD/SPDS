package boomerang.seedfactory;

import boomerang.Query;
import boomerang.WeightedForwardQuery;
import boomerang.jimple.Statement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.*;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by johannesspath on 07.12.17.
 */
public abstract class SeedFactory<W extends Weight> {

    private final WeightedPushdownSystem<Statement, INode<Reachable>, Weight.NoWeight> pds = new WeightedPushdownSystem<>();
    private final Multimap<Query,Transition<Statement, INode<Reachable>>> seedToTransition = HashMultimap.create();
    

    private final WeightedPAutomaton<Statement,  INode<Reachable>, Weight.NoWeight> automaton = new WeightedPAutomaton<Statement,  INode<Reachable>, Weight.NoWeight>(wrap(Reachable.entry())) {
        @Override
        public INode<Reachable> createState(INode<Reachable> reachable, Statement loc) {
            return new GeneratedState<>(reachable,loc);
        }

        @Override
        public boolean isGeneratedState(INode<Reachable> reachable) {
            return reachable instanceof GeneratedState;
        }


        @Override
        public Statement epsilon() {
            return Statement.epsilon();
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
	private Collection<Statement> processed = Sets.newHashSet();

    public Collection<Query> computeSeeds(){
        List<SootMethod> entryPoints = Scene.v().getEntryPoints();
        for(SootMethod m : entryPoints){
            for(Unit u : icfg().getStartPointsOf(m)) {
            	automaton.addTransition(new Transition<Statement, INode<Reachable>>(wrap(Reachable.v()),new Statement((Stmt) u, m),automaton.getInitialState()));
            }
        }
        pds.poststar(automaton);
        automaton.registerListener(new WPAUpdateListener<Statement, INode<Reachable>, Weight.NoWeight>() {
            @Override
            public void onWeightAdded(Transition<Statement, INode<Reachable>> t, Weight.NoWeight noWeight, WeightedPAutomaton<Statement, INode<Reachable>, Weight.NoWeight> aut) {
                process(t.getLabel());

                Stmt u = t.getLabel().getUnit().get();
                Collection<SootMethod> calledMethods = (icfg().isCallStmt(u) ? icfg().getCalleesOfCallAt(u)
                        : new HashSet<SootMethod>());
                for(Query seed : generate(t.getLabel().getMethod(), u, calledMethods)){
                    seedToTransition.put(seed, t);
                }
            }
        });
        
        return seedToTransition.keySet();
    }

    protected abstract Collection<? extends Query> generate(SootMethod method, Stmt u, Collection<SootMethod> calledMethods);
	
    private void process(Statement curr) {
    	if(!processed.add(curr))
    		return;
        Unit currUnit = curr.getUnit().get();
        for(Unit succ : icfg().getSuccsOf(currUnit)){
            addNormalRule(curr, new Statement((Stmt) succ, curr.getMethod()));
        }
        if(icfg().isCallStmt(currUnit)){
            for(Unit returnSite : icfg().getSuccsOf(currUnit)){
                for(SootMethod callee : icfg().getCalleesOfCallAt(currUnit)){
                    for(Unit sp : icfg().getStartPointsOf(callee)){
                        addPushRule(curr, new Statement((Stmt) sp, callee),new Statement((Stmt)returnSite, curr.getMethod()));
                    }
                }
            }
        }
    }

    private void addPushRule(Statement curr, Statement calleeSp, Statement returnSite) {
        pds.addRule(new PushRule<>(wrap(Reachable.v()),curr,wrap(Reachable.v()),calleeSp,returnSite, Weight.NO_WEIGHT_ONE));
    }

    private void addNormalRule(Statement curr, Statement succ) {
        pds.addRule(new NormalRule<>(wrap(Reachable.v()),curr,wrap(Reachable.v()),succ, Weight.NO_WEIGHT_ONE));
    }

    private INode<Reachable> wrap(Reachable r){
        return new SingleNode<>(r);
    }

    public abstract BiDiInterproceduralCFG<Unit,SootMethod> icfg();

	public Collection<SootMethod> getMethodScope(Query query) {
		Set<SootMethod> scope = Sets.newHashSet();
		for(Transition<Statement, INode<Reachable>> t : seedToTransition.get(query)){
			scope.add(t.getLabel().getMethod());
			automaton.registerListener(new TransitiveClosure(t.getTarget(), scope));
		}
		return scope;		
	}

	private class TransitiveClosure extends WPAStateListener<Statement, INode<Reachable>, Weight.NoWeight>{

		private final Set<SootMethod> scope;

		public TransitiveClosure(INode<Reachable> start, Set<SootMethod> scope) {
			super(start);
			this.scope = scope;
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Reachable>> t, NoWeight w,
				WeightedPAutomaton<Statement, INode<Reachable>, NoWeight> weightedPAutomaton) {
			scope.add(t.getLabel().getMethod());
			automaton.registerListener(new TransitiveClosure(t.getTarget(), scope));
		}

		@Override
		public void onInTransitionAdded(Transition<Statement, INode<Reachable>> t, NoWeight w,
				WeightedPAutomaton<Statement, INode<Reachable>, NoWeight> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((scope == null) ? 0 : scope.hashCode());
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
			if (scope == null) {
				if (other.scope != null)
					return false;
			} else if (!scope.equals(other.scope))
				return false;
			return true;
		}

		private SeedFactory getOuterType() {
			return SeedFactory.this;
		}
	}
}
