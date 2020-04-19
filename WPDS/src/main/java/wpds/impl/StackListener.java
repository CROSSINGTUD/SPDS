package wpds.impl;

import com.google.common.collect.Sets;
import java.util.Set;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.interfaces.WPAStateListener;

public abstract class StackListener<N extends Location, D extends State, W extends Weight>
    extends WPAStateListener<N, D, W> {
  /** */
  private final WeightedPAutomaton<N, D, W> aut;

  private N source;
  private Set<N> notifiedStacks = Sets.newHashSet();

  public StackListener(WeightedPAutomaton<N, D, W> weightedPAutomaton, D state, N source) {
    super(state);
    this.aut = weightedPAutomaton;
    this.source = source;
  }

  @Override
  public void onOutTransitionAdded(
      Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
    if (t.getLabel().equals(aut.epsilon())) return;
    if (this.aut.getInitialStates().contains(t.getTarget())) {
      if (t.getLabel().equals(source)) {
        anyContext(source);
      }
      return;
    }
    if (this.aut.isGeneratedState(t.getTarget())) {
      aut.registerListener(new SubStackListener(t.getTarget(), this));
    }
  }

  @Override
  public void onInTransitionAdded(
      Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {}

  public abstract void stackElement(N callSite);

  public abstract void anyContext(N end);

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    StackListener<N, D, W> other = (StackListener<N, D, W>) obj;
    if (source == null) {
      if (other.source != null) return false;
    } else if (!source.equals(other.source)) return false;
    return true;
  }

  private class SubStackListener extends WPAStateListener<N, D, W> {
    private StackListener parent;

    public SubStackListener(D state, StackListener parent) {
      super(state);
      this.parent = parent;
    }

    @Override
    public void onOutTransitionAdded(
        Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {
      if (t.getLabel().equals(aut.epsilon())) return;
      stackElement(t.getLabel());
      if (aut.isGeneratedState(t.getTarget()) && !t.getTarget().equals(t.getStart())) {
        aut.registerListener(new SubStackListener(t.getTarget(), parent));
      }
    }

    @Override
    public void onInTransitionAdded(
        Transition<N, D> t, W w, WeightedPAutomaton<N, D, W> weightedPAutomaton) {}

    public void stackElement(N parent) {
      if (notifiedStacks.add(parent)) {
        StackListener.this.stackElement(parent);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((parent == null) ? 0 : parent.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      SubStackListener other = (SubStackListener) obj;
      if (parent == null) {
        if (other.parent != null) return false;
      } else if (!parent.equals(other.parent)) return false;
      return true;
    }
  }
}
