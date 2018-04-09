package boomerang.poi;

import boomerang.jimple.Field;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.WPAStateListener;
import wpds.interfaces.WPAUpdateListener;

public class ExecuteImportCallStmtPOI<W extends Weight> extends AbstractExecuteImportPOI<W>{

	private final Val returningFact;

	public ExecuteImportCallStmtPOI(AbstractBoomerangSolver<W> baseSolver, AbstractBoomerangSolver<W> flowSolver,
			Statement callSite, Node<Statement, Val> returnedNode) {
		super(baseSolver, flowSolver, callSite, returnedNode.stmt());
		this.returningFact = returnedNode.fact();
	}

	public void solve() {
		baseSolver.getFieldAutomaton().registerListener(new WPAUpdateListener<Field, INode<Node<Statement,Val>>, W>() {

			@Override
			public void onWeightAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
					WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> aut) {
				final INode<Node<Statement, Val>> aliasedVariableAtStmt = t.getStart();
				if(!t.getStart().fact().stmt().equals(succ))
					return;
				
				if(flowSolver instanceof ForwardBoomerangSolver) {
					if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
						Val alias = aliasedVariableAtStmt.fact().fact();
						if (alias.equals(returningFact) && t.getLabel().equals(Field.empty())) {
							// t.getTarget is the allocation site
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
									.getFieldAutomaton();
							baseAutomaton.registerListener(new ImportBackwards(t.getTarget(), new DirectCallback(t.getStart())));
						}
						if (alias.equals(returningFact) && !t.getLabel().equals(Field.empty()) && !t.getLabel().equals(Field.epsilon())) {
							// t.getTarget is the allocation site
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
									.getFieldAutomaton();
							/**
							 * TODO this is the performance bottleneck. Can we implemented a different solution?
							 * when we comment out the line below, the test case
							 * @test.cases.sets.HashMapsLongTest terminated within 110sec.
							 * otherwise a time out occurs after 300sec.
							 * Carefully investigate why we need the line below:
							 * Some of the test cases of 
							 * @test.cases.fields.ThreeFieldsTest fail.
							 */
							baseAutomaton.registerListener(new TransitiveVisitor(t.getTarget()));
						}
					}
				} else {
					if (!(aliasedVariableAtStmt instanceof GeneratedState)) {
						Val alias = aliasedVariableAtStmt.fact().fact();
						if (alias.equals(returningFact) && t.getLabel().equals(Field.empty())) {
							// t.getTarget is the allocation site
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> baseAutomaton = baseSolver
									.getFieldAutomaton();
							baseAutomaton.registerListener(new ImportBackwards(t.getTarget(), new DirectCallback(t.getStart())));
						}
						if (alias.equals(returningFact) && !t.getLabel().equals(Field.empty())) {
							//1. Let curr transition be: (s,l,t)
							//2. Find any transition with (s,l,y) in flowAutomaton
							//3. Take all incoming transitions (q,m,t) in base and add them to flowAutomaton as (q,m,y)
							//4. Import backward subgraph starting at node q  into flowAutomaton 
							//5. Make a step in both automaton: Find n,r,u: (t,n,r) in base (y,n,u) in flow and go back to 3.
							
							baseAutomaton.registerListener(new IntersectionListener(t.getStart(), t.getStart(), t.getLabel(), new IntersectionCallback() {

								@Override
								public void trigger(Transition<Field, INode<Node<Statement, Val>>> baseT,
										Transition<Field, INode<Node<Statement, Val>>> flowT) {
									//3.
									baseAutomaton.registerListener(new Import(t.getTarget(),flowT.getTarget()));
									baseAutomaton.registerListener(new IntersectionListenerNoLabel(t.getTarget(),flowT.getTarget()));
								}
							}));
						}
					}
				}
				
			}
		});
	}
	
	private class Import extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		private INode<Node<Statement, Val>> flowTarget;

		public Import(INode<Node<Statement, Val>> state, INode<Node<Statement, Val>> flowTarget) {
			super(state);
			this.flowTarget = flowTarget;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if (!(t.getStart() instanceof GeneratedState)/** && t.getStart().fact().stmt().equals(curr)**/
					&& !t.getStart().fact().fact().equals(returningFact)) {
				Val alias = t.getStart().fact().fact();
				Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
				Node<Statement, Val> rightOpNode = new Node<Statement, Val>(succ, returningFact);
				flowSolver.setFieldContextReachable(aliasedVarAtSucc);
				flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
			}
			if(t.getLabel().equals(Field.empty())){
				flowAutomaton.registerListener(new WPAStateListener<Field, INode<Node<Statement, Val>>,W>(flowTarget){

					@Override
					public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> innerT, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
						flowAutomaton.addTransition(new Transition<Field, INode<Node<Statement,Val>>>(t.getStart(), innerT.getLabel(), innerT.getTarget()));
					}

					@Override
					public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
						
					}
					@Override
					public int hashCode() {
						return System.identityHashCode(this);
					}
				
				});
				return;
			}
			flowAutomaton.addTransition(new Transition<Field, INode<Node<Statement,Val>>>(t.getStart(), t.getLabel(), flowTarget));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((flowTarget == null) ? 0 : flowTarget.hashCode());
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
			Import other = (Import) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowTarget == null) {
				if (other.flowTarget != null)
					return false;
			} else if (!flowTarget.equals(other.flowTarget))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
	
	}
	
	private class IntersectionListener extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		private INode<Node<Statement, Val>> flowState;
		private Field label;
		private IntersectionCallback callback;

		public IntersectionListener(INode<Node<Statement, Val>> baseState, INode<Node<Statement, Val>> flowState, Field label, IntersectionCallback callback) {
			super(baseState);
			this.flowState = flowState;
			this.label = label;
			this.callback = callback;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> baseT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(!baseT.getLabel().equals(label))
				return;
			flowAutomaton.registerListener(new WPAStateListener<Field, INode<Node<Statement, Val>>, W>(flowState) {

				@Override
				public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
					if(flowT.getLabel().equals(label)) {
						callback.trigger(baseT,flowT);
					}
				}

				@Override
				public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
				}
				@Override
				public int hashCode() {
					return System.identityHashCode(this);
				}
			});
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((flowState == null) ? 0 : flowState.hashCode());
			result = prime * result + ((label == null) ? 0 : label.hashCode());
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
			IntersectionListener other = (IntersectionListener) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowState == null) {
				if (other.flowState != null)
					return false;
			} else if (!flowState.equals(other.flowState))
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
	}
	
	
	private class IntersectionListenerNoLabel extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		private INode<Node<Statement, Val>> flowState;
		private IntersectionCallback callback;

		public IntersectionListenerNoLabel(INode<Node<Statement, Val>> baseState, INode<Node<Statement, Val>> flowState) {
			super(baseState);
			this.flowState = flowState;
			this.callback = new IntersectionCallback() {
				@Override
				public void trigger(Transition<Field, INode<Node<Statement, Val>>> baseT,
						Transition<Field, INode<Node<Statement, Val>>> flowT) {
					//3.
					baseAutomaton.registerListener(new Import(baseT.getTarget(),flowT.getTarget()));
					baseAutomaton.registerListener(new IntersectionListenerNoLabel(baseT.getTarget(), flowT.getTarget()));
				}
			};
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> baseT, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(baseT.getLabel().equals(Field.empty())) {
				flowAutomaton.registerListener(new WPAStateListener<Field, INode<Node<Statement, Val>>, W>(flowState) {

					@Override
					public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
					}
	
					@Override
					public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
							WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
						callback.trigger(baseT,t);
					}
					@Override
					public int hashCode() {
						return System.identityHashCode(this);
					}
				});
				return;
			}
			flowAutomaton.registerListener(new WPAStateListener<Field, INode<Node<Statement, Val>>, W>(flowState) {

				@Override
				public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> flowT, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
					if(flowT.getLabel().equals(baseT.getLabel())) {
						callback.trigger(baseT,flowT);
					}
				}

				@Override
				public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
						WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
				}
				@Override
				public int hashCode() {
					return System.identityHashCode(this);
				}
			}
					);
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((flowState == null) ? 0 : flowState.hashCode());
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
			IntersectionListenerNoLabel other = (IntersectionListenerNoLabel) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (flowState == null) {
				if (other.flowState != null)
					return false;
			} else if (!flowState.equals(other.flowState))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
	}
	
	private interface IntersectionCallback{
		void trigger(Transition<Field, INode<Node<Statement, Val>>> baseT,
				Transition<Field, INode<Node<Statement, Val>>> flowT);	
	}
	
	private class TransitiveVisitor extends WPAStateListener<Field, INode<Node<Statement, Val>>, W>{

		public TransitiveVisitor(INode<Node<Statement, Val>> state) {
			super(state);
			baseSolver
				.getFieldAutomaton().registerListener(new ImportBackwards(state, new FieldStackCallback(state)));
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.epsilon()))
				return;
			if(t.getLabel().equals(Field.empty())) {
				baseSolver
					.getFieldAutomaton().registerListener(new ImportBackwards(t.getTarget(), new FieldStackCallback(t.getStart())));
			} else {
				baseSolver.getFieldAutomaton().registerListener(new TransitiveVisitor(t.getTarget()));
			}
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
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
			TransitiveVisitor other = (TransitiveVisitor) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}
		
	}

	private class FieldStackCallback implements Callback{

		private INode<Node<Statement, Val>> start;

		public FieldStackCallback(INode<Node<Statement, Val>> start) {
			this.start = start;
		}

		@Override
		public void trigger(Transition<Field, INode<Node<Statement, Val>>> t) {
			flowSolver.getFieldAutomaton().addTransition(new Transition<>(t.getStart(),t.getLabel(),start));
		}
		
	}
	// COPIED 

	private class ImportBackwards extends WPAStateListener<Field, INode<Node<Statement, Val>>, W> {

		private Callback callback;

		public ImportBackwards(INode<Node<Statement, Val>> iNode, Callback callback) {
			super(iNode);
			this.callback = callback;
		}

		@Override
		public void onOutTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
		}

		@Override
		public void onInTransitionAdded(Transition<Field, INode<Node<Statement, Val>>> t, W w,
				WeightedPAutomaton<Field, INode<Node<Statement, Val>>, W> weightedPAutomaton) {
			if(t.getLabel().equals(Field.epsilon()))
				return;
			if (!(t.getStart() instanceof GeneratedState) && t.getStart().fact().stmt().equals(curr)
					&& !t.getStart().fact().fact().equals(returningFact)) {
				Val alias = t.getStart().fact().fact();
				Node<Statement, Val> aliasedVarAtSucc = new Node<Statement, Val>(succ, alias);
				Node<Statement, Val> rightOpNode = new Node<Statement, Val>(succ, returningFact);
				callback.trigger(new Transition<Field, INode<Node<Statement, Val>>>(
						new SingleNode<Node<Statement, Val>>(aliasedVarAtSucc), t.getLabel(), t.getTarget()));
				flowSolver.setFieldContextReachable(aliasedVarAtSucc);
				flowSolver.addNormalCallFlow(rightOpNode, aliasedVarAtSucc);
			}
			if (t.getStart() instanceof GeneratedState) {
				baseSolver.getFieldAutomaton().registerListener(new ImportBackwards(t.getStart(),new TransitiveCallback(callback, t)));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
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
			ImportBackwards other = (ImportBackwards) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			return true;
		}

		private ExecuteImportCallStmtPOI getOuterType() {
			return ExecuteImportCallStmtPOI.this;
		}

	}
	
	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((returningFact == null) ? 0 : returningFact.hashCode());
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
		ExecuteImportCallStmtPOI other = (ExecuteImportCallStmtPOI) obj;
		if (returningFact == null) {
			if (other.returningFact != null)
				return false;
		} else if (!returningFact.equals(other.returningFact))
			return false;
		return true;
	}
	
}
