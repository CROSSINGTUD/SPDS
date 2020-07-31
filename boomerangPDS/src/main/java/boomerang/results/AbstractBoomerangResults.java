package boomerang.results;

import boomerang.ForwardQuery;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.util.DefaultValueMap;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  protected final DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>> queryToSolvers;
  protected final Logger LOGGER = LoggerFactory.getLogger(AbstractBoomerangResults.class);

  public AbstractBoomerangResults(
      DefaultValueMap<ForwardQuery, ForwardBoomerangSolver<W>> solverMap) {
    this.queryToSolvers = solverMap;
  }

  protected Context constructContextGraph(
      ForwardQuery forwardQuery, Node<Statement, Val> targetFact) {
    Context context = new Context(targetFact, forwardQuery);
    AbstractBoomerangSolver<W> forwardSolver = queryToSolvers.get(forwardQuery);
    computeUnmatchedOpeningContext(context, forwardSolver, targetFact);
    computeUnmatchedClosingContext(context, forwardSolver);
    return context;
  }

  public void computeUnmatchedClosingContext(
      Context context, AbstractBoomerangSolver<W> forwardSolver) {
    for (Transition<Statement, INode<Val>> t : forwardSolver.getCallAutomaton().getTransitions()) {
      if (t.getTarget().fact().isUnbalanced()) {
        INode<Val> v = t.getTarget();
        forwardSolver
            .getCallAutomaton()
            .registerListener(new ClosingCallStackExtracter<W>(v, v, context, forwardSolver));
      }
    }
  }

  public void computeUnmatchedOpeningContext(
      Context context, AbstractBoomerangSolver<W> forwardSolver, Node<Statement, Val> node) {
    SingleNode<Val> initialState = new SingleNode<>(node.fact());
    forwardSolver
        .getCallAutomaton()
        .registerListener(
            new OpeningCallStackExtracter<>(initialState, initialState, context, forwardSolver));
  }

  public Table<Statement, Val, W> asStatementValWeightTable(ForwardQuery query) {
    final Table<Statement, Val, W> results = HashBasedTable.create();
    Stopwatch sw = Stopwatch.createStarted();
    LOGGER.trace("Computing final weighted results for {}", query);
    WeightedPAutomaton<Statement, INode<Val>, W> callAut =
        queryToSolvers.getOrCreate(query).getCallAutomaton();
    for (Entry<Transition<Statement, INode<Val>>, W> e :
        callAut.getTransitionsToFinalWeights().entrySet()) {
      Transition<Statement, INode<Val>> t = e.getKey();
      W w = e.getValue();
      if (t.getLabel().equals(Statement.epsilon())) continue;
      if (t.getStart().fact().isLocal()
          && !t.getLabel().getMethod().equals(t.getStart().fact().m())) continue;
      results.put(t.getLabel(), t.getStart().fact(), w);
    }
    LOGGER.trace("Computed final weighted results for {} in {}", query, sw);
    return results;
  }

  private static class OpeningCallStackExtracter<W extends Weight>
      extends WPAStateListener<Statement, INode<Val>, W> {

    private AbstractBoomerangSolver<W> solver;
    private INode<Val> source;
    private Context context;

    public OpeningCallStackExtracter(
        INode<Val> state, INode<Val> source, Context context, AbstractBoomerangSolver<W> solver) {
      super(state);
      this.source = source;
      this.context = context;
      this.solver = solver;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
      if (weightedPAutomaton.getInitialStates().contains(t.getTarget())) {
        return;
      }

      // TODO Doesn't work anymore!
      if (t.getLabel().getMethod() != null) {
        if (t.getStart() instanceof GeneratedState) {
          context
              .getOpeningContext()
              .addTransition(new Transition<>(source, t.getLabel(), t.getTarget()));
        } else {
          weightedPAutomaton.registerListener(
              new OpeningCallStackExtracter<>(t.getTarget(), source, context, solver));
          return;
        }
      }
      weightedPAutomaton.registerListener(
          new OpeningCallStackExtracter<>(t.getTarget(), t.getTarget(), context, solver));
    }

    @Override
    public void onInTransitionAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {}

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((context == null) ? 0 : context.hashCode());
      result = prime * result + ((solver == null) ? 0 : solver.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      OpeningCallStackExtracter other = (OpeningCallStackExtracter) obj;
      if (context == null) {
        if (other.context != null) return false;
      } else if (!context.equals(other.context)) return false;
      if (solver == null) {
        if (other.solver != null) return false;
      } else if (!solver.equals(other.solver)) return false;
      if (source == null) {
        if (other.source != null) return false;
      } else if (!source.equals(other.source)) return false;
      return true;
    }
  }

  private static class ClosingCallStackExtracter<W extends Weight>
      extends WPAStateListener<Statement, INode<Val>, W> {

    private AbstractBoomerangSolver<W> solver;
    private INode<Val> source;
    private Context context;

    public ClosingCallStackExtracter(
        INode<Val> state, INode<Val> source, Context context, AbstractBoomerangSolver<W> solver) {
      super(state);
      this.source = source;
      this.context = context;
      this.solver = solver;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {}

    @Override
    public void onInTransitionAdded(
        Transition<Statement, INode<Val>> t,
        W w,
        WeightedPAutomaton<Statement, INode<Val>, W> weightedPAutomaton) {
      if (weightedPAutomaton.isUnbalancedState(t.getStart())) {
        if (!t.getStart().fact().isStatic()) {
          context.getClosingContext().addTransition(t);
        }
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((context == null) ? 0 : context.hashCode());
      result = prime * result + ((solver == null) ? 0 : solver.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      ClosingCallStackExtracter other = (ClosingCallStackExtracter) obj;
      if (context == null) {
        if (other.context != null) return false;
      } else if (!context.equals(other.context)) return false;
      if (solver == null) {
        if (other.solver != null) return false;
      } else if (!solver.equals(other.solver)) return false;
      if (source == null) {
        if (other.source != null) return false;
      } else if (!source.equals(other.source)) return false;
      return true;
    }
  }

  public static class Context {
    final Node<Statement, Val> node;
    private final PAutomaton<Statement, INode<Val>> openingContext;
    private final PAutomaton<Statement, INode<Val>> closingContext;

    public Context(Node<Statement, Val> node, ForwardQuery forwardQuery) {
      this.node = node;
      this.openingContext =
          new PAutomaton<Statement, INode<Val>>() {

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
      this.closingContext =
          new PAutomaton<Statement, INode<Val>>() {

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

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((closingContext == null) ? 0 : closingContext.hashCode());
      result = prime * result + ((node == null) ? 0 : node.hashCode());
      result = prime * result + ((openingContext == null) ? 0 : openingContext.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Context other = (Context) obj;
      if (node == null) {
        if (other.node != null) return false;
      } else if (!node.equals(other.node)) return false;
      return true;
    }

    public PAutomaton<Statement, INode<Val>> getOpeningContext() {
      return openingContext;
    }

    public PAutomaton<Statement, INode<Val>> getClosingContext() {
      return closingContext;
    }
  }
}
