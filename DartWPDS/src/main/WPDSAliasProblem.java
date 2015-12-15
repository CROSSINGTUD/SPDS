package main;

import java.util.Map;
import java.util.Set;

import data.Fact;
import data.One;
import data.PDSSet;
import data.Zero;
import heros.DefaultSeeds;
import heros.FlowFunctions;
import heros.JoinLattice;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import wpds.WEdgeFunctions;
import wpds.WPDSTabulationProblem;

public class WPDSAliasProblem implements
    WPDSTabulationProblem<Unit, Fact, SootMethod, PDSSet, JimpleBasedInterproceduralCFG> {

  private Unit eps;
  private final JimpleBasedInterproceduralCFG icfg;
  private SootMethod startMethod;

  public WPDSAliasProblem(JimpleBasedInterproceduralCFG icfg, SootMethod startMethod) {
    this.icfg = icfg;
    this.startMethod = startMethod;
  }

  @Override
  public FlowFunctions<Unit, Fact, SootMethod> flowFunctions() {
    return new AliasFlowFunctions();
  }

  @Override
  public JimpleBasedInterproceduralCFG interproceduralCFG() {
    return icfg;
  }

  @Override
  public Map<Unit, Set<Fact>> initialSeeds() {
    return DefaultSeeds.make(icfg.getStartPointsOf(startMethod), zeroValue());
  }


  @Override
  public Fact zeroValue() {
    return Fact.REACHABLE;
  }

  @Override
  public boolean autoAddZero() {
    return true;
  }

  @Override
  public WEdgeFunctions<Unit, Fact, SootMethod, PDSSet> edgeFunctions() {
    return new AliasWeightFunctions();
  }

  @Override
  public JoinLattice<PDSSet> joinLattice() {
    return new JoinLattice<PDSSet>() {

      @Override
      public PDSSet topElement() {
        return One.v();
      }

      @Override
      public PDSSet bottomElement() {
        return Zero.v();
      }

      @Override
      public PDSSet join(PDSSet left, PDSSet right) {
        throw new RuntimeException("Never call this");
      }
    };
  }


  @Override
  public Fact createIntermediateState(Fact d, Unit n) {
    return new Fact(d, n);
  }

  @Override
  public Unit epsilon() {
    if (eps == null)
      eps = Jimple.v().newNopStmt();
    return eps;
  }

}
