package boomerang;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.BackwardBoomerangSolver;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class UnbalancedPopHandler<W extends Weight> {
    private INode<Val> returningFact;
    private Transition<Statement, INode<Val>> trans;
    private W weight;

    public UnbalancedPopHandler(INode<Val> returningFact, Transition<Statement, INode<Val>> trans, W weight) {
        this.returningFact = returningFact;
        this.trans = trans;
        this.weight = weight;
    }

    public void trigger(Statement callStatement, AbstractBoomerangSolver<W> solver) {
        boolean valueUsedInStatement = solver.valueUsedInStatement((Stmt) callStatement.getUnit().get(),
                returningFact.fact());
        if (valueUsedInStatement
                || AbstractBoomerangSolver.assignsValue((Stmt) callStatement.getUnit().get(), returningFact.fact())) {
            unbalancedReturnFlow(callStatement, returningFact, trans, weight, solver);
        }
    }

    private void unbalancedReturnFlow(final Statement callStatement, final INode<Val> returningFact,
            final Transition<Statement, INode<Val>> trans, final W weight, AbstractBoomerangSolver<W> solver) {
        solver.submit(callStatement.getMethod(), new Runnable() {
            @Override
            public void run() {
                // Should be moved to BackwardBoomerangSolver/ForwardBoomerangSolver
                if (solver instanceof BackwardBoomerangSolver) {
                    for (Statement returnSite : solver.getSuccsOf(callStatement)) {
                        Val unbalancedFact = returningFact.fact().asUnbalanced(returnSite);
                        SingleNode<Val> unbalancedState = new SingleNode<Val>(unbalancedFact);
                        solver.getCallAutomaton().addUnbalancedState(unbalancedState);
                        solver.getCallAutomaton().addWeightForTransition(
                                new Transition<Statement, INode<Val>>(trans.getTarget(), returnSite, unbalancedState),
                                solver.getCallAutomaton().getOne());
                    }
                } else {
                    Val unbalancedFact = returningFact.fact().asUnbalanced(callStatement);
                    SingleNode<Val> unbalancedState = new SingleNode<Val>(unbalancedFact);
                    solver.getCallAutomaton().addUnbalancedState(unbalancedState);
                    solver.getCallAutomaton().addWeightForTransition(
                            new Transition<Statement, INode<Val>>(trans.getTarget(), callStatement, unbalancedState),
                            solver.getCallAutomaton().getOne());
                }
            }

        });
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((returningFact == null) ? 0 : returningFact.hashCode());
        result = prime * result + ((trans == null) ? 0 : trans.hashCode());
        result = prime * result + ((weight == null) ? 0 : weight.hashCode());
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
        UnbalancedPopHandler<W> other = (UnbalancedPopHandler<W>) obj;
        if (returningFact == null) {
            if (other.returningFact != null)
                return false;
        } else if (!returningFact.equals(other.returningFact))
            return false;
        if (trans == null) {
            if (other.trans != null)
                return false;
        } else if (!trans.equals(other.trans))
            return false;
        if (weight == null) {
            if (other.weight != null)
                return false;
        } else if (!weight.equals(other.weight))
            return false;
        return true;
    }
}