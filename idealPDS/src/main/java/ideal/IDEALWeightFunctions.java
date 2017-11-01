package ideal;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import ideal.PerSeedAnalysisContext.Phases;
import sync.pds.solver.WeightFunctions;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

public class IDEALWeightFunctions<W extends Weight> implements WeightFunctions<Statement,Val,Statement,W> {

	private WeightFunctions<Statement,Val,Statement,W> delegate;
	private Set<NonOneFlowListener<W>> listeners = Sets.newHashSet(); 
	private Map<Statement, W> potentialStrongUpdates = Maps.newHashMap();
	private Set<Statement> weakUpdates = Sets.newHashSet();
	private Multimap<Node<Statement,Val>, W> nonOneFlowNodes = HashMultimap.create();
	private Phases phase; 

	public IDEALWeightFunctions(WeightFunctions<Statement,Val,Statement,W>  delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public W push(Node<Statement, Val> curr, Node<Statement, Val> succ, Statement calleeSp) {
		W weight = delegate.push(curr, succ, calleeSp);
		if (isObjectFlowPhase() &&!weight.equals(getOne())){	
			addOtherThanOneWeight(curr, weight);
		}
		return weight;
	}

	public void addOtherThanOneWeight(Node<Statement, Val> curr, W weight) {
		if(nonOneFlowNodes.put(curr, weight)){
			for(NonOneFlowListener<W> l : Lists.newArrayList(listeners)){
				l.nonOneFlow(curr,weight);
			}
		}
	}

	@Override
	public W normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		
		W weight = delegate.normal(curr, succ);
		
		if(isValueFlowPhase() && IDEALAnalysis.ENABLE_STRONG_UPDATES){
			if(potentialStrongUpdates.containsKey(curr.stmt())){
				W w = potentialStrongUpdates.get(curr.stmt());
				System.err.println("Potential strong update "+ curr + "  " + w);
				if(!weakUpdates.contains(curr.stmt())){
					System.err.println("Strong update " + curr + w);
					return w;
				}
				weight = (W) weight.combineWith(w);
				System.err.println("No strong update" + weight);
			}
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
		W weight = delegate.pop(curr, location);
//		if (isObjectFlowPhase() && !weight.equals(getOne())){
//			addOtherThanOneWeight(curr, weight);
//		}
		return weight;
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


//	@Override
//	public EdgeFunction<TypestateDomainValue<State>> getCallToReturnEdgeFunction(AccessGraph d1, Unit callSite, AccessGraph d2,
//			Unit returnSite, AccessGraph d3) {
//		Set<? extends Transition<State>> trans = func.getCallToReturnTransitionsFor(d1, callSite, d2, returnSite, d3);
//		if (trans.isEmpty())
//			return EdgeIdentity.v();
//		return new TransitionFunction<State>(trans);
//	}

	
	@Override
	public String toString() {
		return "[IDEAL-Wrapped Weights] " + delegate.toString();
	}

	public void potentialStrongUpdate(Statement stmt, W weight) {
		W w = potentialStrongUpdates.get(stmt);
		W newWeight = (w == null ? weight : (W) w.combineWith(weight)); 
		potentialStrongUpdates.put(stmt, newWeight);
	}
	
	public void weakUpdate(Statement stmt) {
		weakUpdates.add(stmt);
	}

	public void setPhase(Phases phase) {
		this.phase = phase;
	}
}
