package main;

import java.util.Collections;

import data.AccessStmt;
import data.Fact;
import data.FieldPAutomaton;
import data.PDSSet;
import data.PDSSet.PDS;
import data.WrappedSootField;
import pathexpression.RegEx;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import wpds.WPDSSolver;
import wpds.impl.PushdownSystem;
import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;

public class WPDSMain {
  private JimpleBasedInterproceduralCFG icfg;
  private PushdownSystem<Unit, Fact, PDSSet> pds;
  public WPDSMain() {
    icfg = new JimpleBasedInterproceduralCFG();
    WPDSSolver<Unit, Fact, SootMethod, PDSSet, JimpleBasedInterproceduralCFG> wpdsSolver =
        new WPDSSolver<>(new WPDSAliasProblem(icfg, Scene.v().getMainMethod()));
    wpdsSolver.submitInitialSeeds();
    pds = wpdsSolver.getPushdownSystem();
    System.out.println(pds);
  }
  public void query(Fact fact, Unit stmt) {
    System.out.println("QUERY " + fact + "@" + stmt);
    UnitPAutomaton pAutomaton = new UnitPAutomaton(fact,
        Collections.singleton(new Transition<Unit, Fact>(fact, stmt, Fact.TARGET)),
        Fact.TARGET);

    WeightedPAutomaton<Unit, Fact, PDSSet> prestar = pds.prestar(pAutomaton);
    System.out.println(prestar);
    for (Transition<Unit, Fact> t : prestar.getTransitions()) {
      Unit unit = t.getString();
      if (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof NewExpr
          && t.getStart().equals(new Fact((Local) ((AssignStmt) unit).getLeftOp()))) {
        if (isAllocationSiteFor(fact, stmt, prestar, t)) {
          System.out.println("ALLOC  " + unit);
        }
      }
    }
  }

  private boolean isAllocationSiteFor(Fact fact, Unit stmt,
      WeightedPAutomaton<Unit, Fact, PDSSet> prestar, Transition<Unit, Fact> t) {
    PDSSet weight = prestar.getWeightFor(t);
    AccessStmt accessStmt = new AccessStmt(stmt);
    for (PDS pds : weight.getPDSSystems()) {
      FieldPAutomaton fieldPAutomaton = new FieldPAutomaton(accessStmt,
          Collections.singleton(new Transition<WrappedSootField, AccessStmt>(accessStmt,
              WrappedSootField.EPSILON, AccessStmt.TARGET)),
          AccessStmt.TARGET);
      RegEx<WrappedSootField> language = pds.prestar(fieldPAutomaton).extractLanguage(new AccessStmt(t.getLabel()));
      if (RegEx.<WrappedSootField>contains(language,
          new RegEx.Plain<WrappedSootField>(WrappedSootField.EPSILON)))
        return true;
    };
    return false;
  }
}
