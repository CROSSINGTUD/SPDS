package boomerang.results;

import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import heros.utilities.DefaultValueMap;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
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
	
	protected Context constructContextGraph(ForwardQuery forwardQuery, Node<Statement,Val> targetFact) {
		AbstractBoomerangResults<W>.Context context = new Context(targetFact, forwardQuery);
		context.computeUnmatchedOpeningContext();
		context.computeUnmatchedClosingContext();
		return context;
	}
	

	private class OpeningCallStackExtracter extends WPAStateListener<Statement, INode<Val>, W>{

		private AbstractBoomerangSolver<W> solver;
		private INode<Val> source;
		private AbstractBoomerangResults<W>.Context context;

		public OpeningCallStackExtracter(INode<Val> state, INode<Val> source,AbstractBoomerangResults<W>.Context context, AbstractBoomerangSolver<W> solver) {
			super(state);
			this.source = source;
			this.context = context;
			this.solver = solver;
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			if(t.getTarget().equals(weightedPAutomaton.getInitialState())) {
				return;
			}
			if(t.getTarget().fact().isUnbalanced()) {
				context.addUnbalancedNodes(t.getTarget());
			}
			if(t.getLabel().getMethod() != null) {
				if(t.getStart() instanceof GeneratedState) {
					context.getOpeningContext().addTransition(new Transition<Statement,INode<Val>>(source,t.getLabel(),t.getTarget()));
				} else {
					weightedPAutomaton.registerListener(new OpeningCallStackExtracter(t.getTarget(),source, context, solver));
					return;
				}
			}
			weightedPAutomaton.registerListener(new OpeningCallStackExtracter(t.getTarget(),t.getTarget(), context, solver));
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
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			result = prime * result + ((solver == null) ? 0 : solver.hashCode());
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
			OpeningCallStackExtracter other = (OpeningCallStackExtracter) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (context == null) {
				if (other.context != null)
					return false;
			} else if (!context.equals(other.context))
				return false;
			if (solver == null) {
				if (other.solver != null)
					return false;
			} else if (!solver.equals(other.solver))
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

	

	private class ClosingCallStackExtracter extends WPAStateListener<Statement, INode<Val>, W>{

		private AbstractBoomerangSolver<W> solver;
		private INode<Val> source;
		private AbstractBoomerangResults<W>.Context context;

		public ClosingCallStackExtracter(INode<Val> state, INode<Val> source,AbstractBoomerangResults<W>.Context context, AbstractBoomerangSolver<W> solver) {
			super(state);
			this.source = source;
			this.context = context;
			this.solver = solver;
		}

		@Override
		public void onOutTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(Transition<Statement, INode<Val>> t, W w,
				WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
			if(weightedPAutomaton.isUnbalancedState(t.getStart())) {
				context.getClosingContext().addTransition(t);
				weightedPAutomaton.registerListener(new ClosingCallStackExtracter(t.getStart(),source, context, solver));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			result = prime * result + ((solver == null) ? 0 : solver.hashCode());
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
			ClosingCallStackExtracter other = (ClosingCallStackExtracter) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (context == null) {
				if (other.context != null)
					return false;
			} else if (!context.equals(other.context))
				return false;
			if (solver == null) {
				if (other.solver != null)
					return false;
			} else if (!solver.equals(other.solver))
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
	public class Context{
		final Node<Statement,Val> node;
		private final PAutomaton<Statement,INode<Val>> openingContext;
		private final PAutomaton<Statement,INode<Val>> closingContext;
		private final Set<INode<Val>> unbalancedClosingContexts = Sets.newHashSet();
		private ForwardQuery forwardQuery;
		public Context(Node<Statement,Val> node, ForwardQuery forwardQuery) {
			this.node = node;
			this.forwardQuery = forwardQuery;
			this.openingContext = new PAutomaton<Statement,INode<Val>>(new SingleNode<Val>(node.fact())) {
		
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
			this.closingContext = new PAutomaton<Statement,INode<Val>>(new SingleNode<Val>(node.fact())) {
				
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
		}
		public void computeUnmatchedClosingContext() {
			for(INode<Val> v : unbalancedClosingContexts) {
				queryToSolvers.get(forwardQuery).getCallAutomaton().registerListener(new ClosingCallStackExtracter(v, v, this, queryToSolvers.get(forwardQuery)));
			}
	
		}
		public void computeUnmatchedOpeningContext() {
			SingleNode<Val> initialState = new SingleNode<Val>(node.fact());
			queryToSolvers.get(forwardQuery).getCallAutomaton().registerListener(new OpeningCallStackExtracter(initialState, initialState, this, queryToSolvers.get(forwardQuery)));
		}
		public void addUnbalancedNodes(INode<Val> target) {
			unbalancedClosingContexts.add(target);
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((closingContext == null) ? 0 : closingContext.hashCode());
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + ((openingContext == null) ? 0 : openingContext.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Context other = (Context) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (node == null) {
				if (other.node != null)
					return false;
			} else if (!node.equals(other.node))
				return false;
			return true;
		}
		private AbstractBoomerangResults getOuterType() {
			return AbstractBoomerangResults.this;
		}
		public PAutomaton<Statement,INode<Val>> getOpeningContext() {
			return openingContext;
		}
		public PAutomaton<Statement,INode<Val>> getClosingContext() {
			return closingContext;
		}
	}
}
