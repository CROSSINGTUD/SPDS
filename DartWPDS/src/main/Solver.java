package main;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import wpds.impl.NormalRule;
import wpds.impl.PAutomaton;
import wpds.impl.PopRule;
import wpds.impl.PreStar;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.Transition;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import data.Access;
import data.AccessStmt;
import data.Fact;
import data.FieldWeight;
import data.One;
import data.Stmt;
import data.WrappedSootField;

public class Solver {
  private JimpleBasedInterproceduralCFG icfg;
  private WPDS pds;
  private Set<Rule<Stmt, Fact, Access>> rules = Sets.newHashSet();
  private LinkedList<Rule<Stmt, Fact, Access>> worklist = Lists.newLinkedList();

  public Solver() {
    icfg = new JimpleBasedInterproceduralCFG();
    pds = new WPDS();
    initPDS();
    System.out.println(pds);
  }

  private void initPDS() {
    SootMethod mainMethod = Scene.v().getMainMethod();
    for (Unit sp : icfg.getStartPointsOf(mainMethod)) {
      worklist.add(new NormalRule<Stmt, Fact, Access>(new Stmt(sp), Fact.REACHABLE, new Stmt(sp),
          Fact.REACHABLE, One.v()));
    }
    process(worklist);
    System.out.println(pds);
  }

  private void process(LinkedList<Rule<Stmt, Fact, Access>> worklist) {
    while (!worklist.isEmpty()) {
      Rule<Stmt, Fact, Access> rule = worklist.removeFirst();
      if (!rule.getS1().equals(Fact.REACHABLE))
        pds.addRule(rule);
      if (!rules.contains(rule)) {
        rules.add(rule);
        computeTargets(rule);
      }
    }
  }

  private void computeTargets(Rule<Stmt, Fact, Access> rule) {
    Stmt l2 = rule.getL2();
    if (l2 == null)
      return;
    for (Unit succ : icfg.getSuccsOf(l2.getDelegate())) {
      if (icfg.isCallStmt(succ)) {
        processCall(succ, rule);
      } else if (icfg.isExitStmt(succ)) {
        processExit(succ, rule);
      } else {
        processNormal(succ, rule);
      }
    }

  }

  private void processCall(Unit succ, Rule<Stmt, Fact, Access> rule) {
    // succ is a callSite
    InvokeExpr invokeExpr = ((soot.jimple.Stmt) succ).getInvokeExpr();
    Local base = null;
    Local[] paramLocals = null;
    if (invokeExpr instanceof VirtualInvokeExpr) {
      VirtualInvokeExpr vie = (VirtualInvokeExpr) invokeExpr;
      base = (Local) vie.getBase();
      paramLocals = new Local[vie.getArgCount()];
      for (int i = 0; i < vie.getArgCount(); i++) {
        paramLocals[i] = (Local) vie.getArg(i);
      }
    }
    for (SootMethod callee : icfg.getCalleesOfCallAt(succ)) {
      for (Unit sp : icfg.getStartPointsOf(callee)) {
        if (rule.getS2().equals(new Fact(base))) {
          PushRule<Stmt, Fact, Access> pushRule =
              new PushRule<Stmt, Fact, Access>(rule.getL2(), rule.getS2(), new Stmt(succ),
                  new Stmt(sp), new Fact(callee.getActiveBody().getThisLocal()), computeWeight(
                      rule.getL2(), succ));
          worklist.add(pushRule);
        }
        if (paramLocals != null)
          for (int i = 0; i < paramLocals.length; i++) {
            if (rule.getS2().equals(new Fact(paramLocals[i]))) {
              PushRule<Stmt, Fact, Access> pushRule =
                  new PushRule<Stmt, Fact, Access>(rule.getL2(), rule.getS2(), new Stmt(succ),
                      new Stmt(sp), new Fact(callee.getActiveBody().getParameterLocal(i)),
                      computeWeight(rule.getL2(), succ));
              worklist.add(pushRule);
            }
          }

      }
    }

    processCallToReturn(succ, rule);
  }

  private void processCallToReturn(Unit succ, Rule<Stmt, Fact, Access> rule) {
    NormalRule<Stmt, Fact, Access> generate =
        new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), rule.getS2(), computeWeight(
            rule.getL2(), succ));
    worklist.add(generate);
  }

  private void processExit(Unit succ, Rule<Stmt, Fact, Access> rule) {
    SootMethod callee = icfg.getMethodOf(succ);
    Collection<Unit> callSites = icfg.getCallersOf(callee);
    int index = -2;
    if (!callee.isStatic() && rule.getS2().equals(new Fact(callee.getActiveBody().getThisLocal()))) {
      index = -1;
    }
    Local[] paramLocals = new Local[callee.getActiveBody().getParameterLocals().size()];
    for (int i = 0; i < paramLocals.length; i++) {
      index = i;
    }
    if (index == -2)
      return;
    for (Unit callSite : callSites) {
      if (!(callSite instanceof soot.jimple.Stmt))
        continue;
      soot.jimple.Stmt inv = (soot.jimple.Stmt) callSite;
      if (index == -1) {
        PopRule<Stmt, Fact, Access> generate =
            new PopRule<>(rule.getL2(), rule.getS2(), new Fact(
                (Local) ((VirtualInvokeExpr) inv.getInvokeExpr()).getBase()), computeWeight(
                rule.getL2(), succ));
        worklist.add(generate);
      } else {
        PopRule<Stmt, Fact, Access> generate =
            new PopRule<>(rule.getL2(), rule.getS2(), new Fact((Local) inv.getInvokeExpr().getArg(
                index)), computeWeight(rule.getL2(), succ));
        worklist.add(generate);
      }
    }
  }

  private void processNormal(Unit succ, Rule<Stmt, Fact, Access> rule) {
    System.out.println("NROMAL " + rule);
    if (succ instanceof AssignStmt) {
      AssignStmt assign = (AssignStmt) succ;
      if (assign.getRightOp() instanceof NewExpr) {
        if (rule.getS2().equals(Fact.REACHABLE)) {
          Fact fact = new Fact((Local) assign.getLeftOp());
          NormalRule<Stmt, Fact, Access> generate =
              new NormalRule<>(rule.getL2(), fact, new Stmt(succ), fact, computeWeight(
                  rule.getL2(), succ));
          worklist.add(generate);
        }
      } else if (assign.getRightOp() instanceof Local) {
        if (rule.getS2().equals(new Fact((Local) assign.getRightOp()))) {
          if (assign.getLeftOp() instanceof Local) {
            Local leftOp = (Local) assign.getLeftOp();
            NormalRule<Stmt, Fact, Access> generate =
                new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), new Fact(leftOp),
                    computeWeight(rule.getL2(), succ));
            worklist.add(generate);
          }
        }
      }
    }
    NormalRule<Stmt, Fact, Access> generate =
        new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), rule.getS2(), computeWeight(
            rule.getL2(), succ));
    worklist.add(generate);
  }

  private Access computeWeight(Stmt prev, Unit succ) {
    if (succ instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) succ;
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
        return new Access(new PushRule<WrappedSootField, AccessStmt, FieldWeight>(
            WrappedSootField.ANYFIELD, new AccessStmt(prev.getDelegate()),
            WrappedSootField.ANYFIELD, new WrappedSootField(ifr.getField()), new AccessStmt(succ),
            new FieldWeight()));
      } else if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
        return new Access(new PopRule<WrappedSootField, AccessStmt, FieldWeight>(
            new WrappedSootField(ifr.getField()), new AccessStmt(prev.getDelegate()),
            new AccessStmt(succ), new FieldWeight()));
      }
    }
    return new Access(new NormalRule<WrappedSootField, AccessStmt, FieldWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(prev.getDelegate()), WrappedSootField.ANYFIELD,
        new AccessStmt(succ), new FieldWeight()));
  }

  public void query(Fact fact, Unit stmt) {
    System.out.println(pds.getStates());
    WPAutomaton pAutomaton =
        new WPAutomaton(Collections.singleton(fact),
            Collections.singleton(new Transition<Stmt, Fact, Access>(fact, new Stmt(stmt),
                Fact.TARGET)), Collections.singleton(Fact.TARGET));
    System.out.println(pAutomaton);
    PreStar<Stmt, Fact, Access> star = new PreStar<Stmt, Fact, Access>();
    System.out.println(star.prestar(pds, pAutomaton));

    PAutomaton<Stmt, Fact, Access> prestar = pds.prestar(pAutomaton);
    for (Transition<Stmt, Fact, Access> t : prestar.getTransitions()) {
      Stmt string = t.getString();
      Unit unit = string.getDelegate();
      if (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof NewExpr) {
        Value leftOp = ((AssignStmt) unit).getLeftOp();
        Fact allocFact = new Fact((Local) leftOp);
        WPAutomaton queryAutomaton =
            new WPAutomaton(Collections.singleton(allocFact),
                Collections.singleton(new Transition<Stmt, Fact, Access>(allocFact, string,
                    Fact.TARGET)), Collections.singleton(Fact.TARGET));
        PAutomaton<Stmt, Fact, Access> poststar = pds.poststar(queryAutomaton);
        System.out.println(poststar);
      }
    }
    // TODO Auto-generated method stub

  }
}
