package main;

import java.util.Collections;

import data.AccessStmt;
import data.Fact;
import data.FieldPAutomaton;
import data.PDSSet;
import data.PDSSet.PDS;
import data.WrappedSootField;
import pathexpression.IRegEx;
import pathexpression.RegEx;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import wpds.WPDSSolver;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.impl.WeightedPAutomaton;
import wpds.impl.WeightedPushdownSystem;

public class WPDSMain {
  private JimpleBasedInterproceduralCFG icfg;
  private WeightedPushdownSystem<Unit, Fact, PDSSet> pds;
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
    for (Transition<Unit, Fact> t : prestar.getTransitions()) {
      Unit allocationSite = t.getString();
      if (allocationSite instanceof AssignStmt && ((AssignStmt) allocationSite).getRightOp() instanceof NewExpr) {
        Fact allocatedObject = new Fact((Local) ((AssignStmt) allocationSite).getLeftOp());
        if (t.getStart().equals(allocatedObject)) {
          if (isAllocationSiteFor(fact, stmt, prestar, t)) {
            System.out.println("ALLOC  " + allocationSite);
            // TODO should keep track of path!
            UnitPAutomaton acceptsAllocation =
                new UnitPAutomaton(allocatedObject,
                    Collections
                        .singleton(new Transition<Unit, Fact>(allocatedObject, allocationSite, Fact.TARGET)),
                Fact.TARGET);
            WeightedPAutomaton<Unit, Fact, PDSSet> poststar = pds.poststar(acceptsAllocation);
            extractFactsReachableAt(stmt, poststar,
                new AccessStmt(allocationSite));
          }
        }
      }
    }
  }

  private void extractFactsReachableAt(Unit stmt, WeightedPAutomaton<Unit, Fact, PDSSet> poststar,
      AccessStmt allocationSite) {
    for (Transition<Unit, Fact> t : poststar.getTransitions()) {
      if (t.getLabel().equals(stmt)) {
        System.out.println(t);
        PDSSet pdssystem = poststar.getWeightFor(t);
        System.out.println(pdssystem);
        for (PDS pds : pdssystem.getPDSSystems()) {
          FieldPAutomaton fieldPAutomaton = new FieldPAutomaton(allocationSite,
              Collections.singleton(new Transition<WrappedSootField, AccessStmt>(allocationSite,
                  WrappedSootField.EPSILON, AccessStmt.TARGET)),
              AccessStmt.TARGET);

          // if (t.getStart().toString().equals("e")) {
            WeightedPAutomaton<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> aur =
              pds.poststar(fieldPAutomaton);
          System.out.print(t.getStart() + ".");
          IRegEx<WrappedSootField> language =
 aur.extractLanguage(new AccessStmt(t.getLabel()));
          System.out.println(language);
          // System.out.println(aur);
          // }
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
      IRegEx<WrappedSootField> language =
          pds.prestar(fieldPAutomaton).extractLanguage(new AccessStmt(t.getLabel()));
      if (RegEx.<WrappedSootField>containsEpsilon(language))
        return true;
    };
    return false;
  }
}
