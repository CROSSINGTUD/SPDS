package boomerang.results;

import java.util.Set;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import heros.utilities.DefaultValueMap;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.PAutomaton;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;

public class AbstractBoomerangResults<W extends Weight> {
	protected final DefaultValueMap<Query, AbstractBoomerangSolver<W>> queryToSolvers;
	public AbstractBoomerangResults(DefaultValueMap<Query, AbstractBoomerangSolver<W>> solverMap) {
		this.queryToSolvers = solverMap;
	}
	protected PAutomaton<Statement, INode<Val>> constructContextGraph(ForwardQuery forwardQuery, Val targetFact) {
		AbstractBoomerangSolver<W> solver = queryToSolvers.get(forwardQuery);
		WeightedPAutomaton<Statement, INode<Val>, W> callAutomaton = queryToSolvers.get(forwardQuery).getCallAutomaton();
		SingleNode<Val> initialState = new SingleNode<Val>(targetFact);
		PAutomaton<Statement,INode<Val>> aut = new PAutomaton<Statement,INode<Val>>(initialState) {

			@Override
			public INode<Val> createState(INode<Val> d, Statement loc) {
				throw new RuntimeException("Not implemented");
			}

			@Override
			public boolean isGeneratedState(INode<Val> d) {
				throw new RuntimeException("Not implemented");
			}

			@Override
			public Statement epsilon() {
				return Statement.epsilon();
			}

		};
		callAutomaton.registerListener(new CallStackExtracter(initialState,initialState,aut, solver));
		return aut;
	}
	

	private class CallStackExtracter extends WPAStateListener<Statement, INode<Val>, W>{

		private AbstractBoomerangSolver<W> solver;
		private INode<Val> source;
		private PAutomaton<Statement, INode<Val>> aut;

		public CallStackExtracter(INode<Val> state, INode<Val> source,PAutomaton<Statement, INode<Val>> aut, AbstractBoomerangSolver<W> solver) {
			super(state);
			this.source = source;
			this.aut = aut;
			this.solver = solver;
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			if(t.getLabel().getMethod() != null) {
				if(t.getStart() instanceof GeneratedState) {
					Set<Statement> succsOf = solver.getPredsOf(t.getLabel());
					for(Statement s : succsOf) {
						aut.addTransition(new Transition<Statement,INode<Val>>(source,s,t.getTarget()));
					}
				} else {
					weightedPAutomaton.registerListener(new CallStackExtracter(t.getTarget(),source, aut, solver));
					return;
				}
			}
			weightedPAutomaton.registerListener(new CallStackExtracter(t.getTarget(),t.getTarget(), aut, solver));
		}

		@Override
		public void onInTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			CallStackExtracter other = (CallStackExtracter) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

		private AbstractBoomerangResults getOuterType() {
			return AbstractBoomerangResults.this;
		}
		
	}
	
}
