package main;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import soot.Local;
import soot.MethodOrMethodContext;
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
import soot.util.queue.QueueReader;
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.WeightedPAutomaton;
import wpds.interfaces.Weight;
import wpds.interfaces.Weight.NoWeight;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import data.AccessStmt;
import data.Fact;
import data.FieldPAutomaton;
import data.One;
import data.PDSSet;
import data.Stmt;
import data.WrappedSootField;

public class Solver {
  private JimpleBasedInterproceduralCFG icfg;
  private WPDS pds;
  private Set<Rule<Stmt, Fact, PDSSet>> rules = Sets.newHashSet();
  private LinkedList<Rule<Stmt, Fact, PDSSet>> worklist = Lists.newLinkedList();

  public Solver() {
    icfg = new JimpleBasedInterproceduralCFG();
    pds = new WPDS();
    initPDS();
  }

  private void initPDS() {
    QueueReader<MethodOrMethodContext> listener = Scene.v().getReachableMethods().listener();
    while (listener.hasNext()) {
      MethodOrMethodContext next = listener.next();
      for (Unit sp : icfg.getStartPointsOf(next.method())) {
        worklist.add(new NormalRule<Stmt, Fact, PDSSet>(new Stmt(sp), Fact.REACHABLE, new Stmt(sp),
            Fact.REACHABLE, One.v()));
      }
    }
    process(worklist);
  }

  private void process(LinkedList<Rule<Stmt, Fact, PDSSet>> worklist) {
    long before = System.currentTimeMillis();
    while (!worklist.isEmpty()) {
      Rule<Stmt, Fact, PDSSet> rule = worklist.removeFirst();
      // System.out.println(rule);
      if (!rule.getS1().equals(Fact.REACHABLE))
        pds.addRule(rule);
      if (!rules.contains(rule)) {
        rules.add(rule);
        computeTargets(rule);
      }
    }
    long after = System.currentTimeMillis();
    System.out.println("PDS Phase: " + (after - before));
  }

  private void computeTargets(Rule<Stmt, Fact, PDSSet> rule) {
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

  private void processCall(Unit succ, Rule<Stmt, Fact, PDSSet> rule) {
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
          PushRule<Stmt, Fact, PDSSet> pushRule =
              new PushRule<Stmt, Fact, PDSSet>(rule.getL2(), rule.getS2(), new Stmt(succ),
                  new Stmt(sp), new Fact(callee.getActiveBody().getThisLocal()), computeWeight(
                      rule.getL2(), succ));
          worklist.add(pushRule);
        }
        if (paramLocals != null)
          for (int i = 0; i < paramLocals.length; i++) {
            if (rule.getS2().equals(new Fact(paramLocals[i]))) {
              PushRule<Stmt, Fact, PDSSet> pushRule =
                  new PushRule<Stmt, Fact, PDSSet>(rule.getL2(), rule.getS2(), new Stmt(succ),
                      new Stmt(sp), new Fact(callee.getActiveBody().getParameterLocal(i)),
                      computeWeight(rule.getL2(), succ));
              worklist.add(pushRule);
            }
          }

      }
    }

    processCallToReturn(succ, rule);
  }

  private void processCallToReturn(Unit succ, Rule<Stmt, Fact, PDSSet> rule) {
    NormalRule<Stmt, Fact, PDSSet> generate =
        new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), rule.getS2(), computeWeight(
            rule.getL2(), succ));
    worklist.add(generate);
  }

  private void processExit(Unit succ, Rule<Stmt, Fact, PDSSet> rule) {
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
        PopRule<Stmt, Fact, PDSSet> generate =
            new PopRule<>(rule.getL2(), rule.getS2(), new Fact(
                (Local) ((VirtualInvokeExpr) inv.getInvokeExpr()).getBase()), computeWeight(
                rule.getL2(), succ));
        worklist.add(generate);
      } else {
        PopRule<Stmt, Fact, PDSSet> generate =
            new PopRule<>(rule.getL2(), rule.getS2(), new Fact((Local) inv.getInvokeExpr().getArg(
                index)), computeWeight(rule.getL2(), succ));
        worklist.add(generate);
      }
    }
  }

  private void processNormal(Unit succ, Rule<Stmt, Fact, PDSSet> rule) {

    if (succ instanceof AssignStmt) {
      AssignStmt assign = (AssignStmt) succ;
      if (assign.getRightOp() instanceof NewExpr) {
        if (rule.getS2().equals(Fact.REACHABLE)) {
          Fact fact = new Fact((Local) assign.getLeftOp());
          NormalRule<Stmt, Fact, PDSSet> generate =
              new NormalRule<>(rule.getL2(), fact, new Stmt(succ), fact, computeWeight(
                  rule.getL2(), succ));
          worklist.add(generate);
        }
      } else if (assign.getRightOp() instanceof Local) {
        if (rule.getS2().equals(new Fact((Local) assign.getRightOp()))) {
          if (assign.getLeftOp() instanceof Local) {
            Local leftOp = (Local) assign.getLeftOp();
            NormalRule<Stmt, Fact, PDSSet> generate =
                new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), new Fact(leftOp),
                    computeWeight(rule.getL2(), succ));
            worklist.add(generate);
          } else if (assign.getLeftOp() instanceof InstanceFieldRef) {
            InstanceFieldRef leftOp = (InstanceFieldRef) assign.getLeftOp();
            NormalRule<Stmt, Fact, PDSSet> generate =
                new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), new Fact(
                    (Local) leftOp.getBase()), computeWeight(rule.getL2(), succ));
            worklist.add(generate);
          }
        }
      } else if (assign.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef instanceFieldRef = (InstanceFieldRef) assign.getRightOp();
        if (rule.getS2().equals(new Fact((Local) instanceFieldRef.getBase()))) {
          if (assign.getLeftOp() instanceof Local) {
            Local leftOp = (Local) assign.getLeftOp();
            NormalRule<Stmt, Fact, PDSSet> generate =
                new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), new Fact(leftOp),
                    computeWeight(rule.getL2(), succ));
            worklist.add(generate);
          }
        }
      }
    }
    NormalRule<Stmt, Fact, PDSSet> generate =
        new NormalRule<>(rule.getL2(), rule.getS2(), new Stmt(succ), rule.getS2(), new PDSSet(
            new NormalRule<WrappedSootField, AccessStmt, NoWeight>(WrappedSootField.ANYFIELD,
                new AccessStmt(rule.getL2().getDelegate()), WrappedSootField.ANYFIELD,
                new AccessStmt(succ), Weight.NO_WEIGHT)));
    worklist.add(generate);
  }

  private PDSSet computeWeight(Stmt prev, Unit succ) {
    if (succ instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) succ;
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
        return new PDSSet(new PushRule<WrappedSootField, AccessStmt, NoWeight>(
            WrappedSootField.ANYFIELD, new AccessStmt(prev.getDelegate()),
            WrappedSootField.ANYFIELD, new WrappedSootField(ifr.getField()), new AccessStmt(succ),
            Weight.NO_WEIGHT));
      } else if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
        return new PDSSet(new PopRule<WrappedSootField, AccessStmt, NoWeight>(new WrappedSootField(
            ifr.getField()), new AccessStmt(prev.getDelegate()), new AccessStmt(succ),
            Weight.NO_WEIGHT));
      }
    }
    return new PDSSet(new NormalRule<WrappedSootField, AccessStmt, NoWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(prev.getDelegate()), WrappedSootField.ANYFIELD,
        new AccessStmt(succ), Weight.NO_WEIGHT));
  }

  public void query(Fact fact, Unit stmt) {
    WPAutomaton pAutomaton =
        new WPAutomaton(fact, Collections.singleton(new Transition<Stmt, Fact>(fact,
            new Stmt(stmt), Fact.TARGET)), Fact.TARGET);

    WeightedPAutomaton<Stmt, Fact, PDSSet> prestar = pds.prestar(pAutomaton);
    for (Transition<Stmt, Fact> t : prestar.getTransitions()) {
      Stmt string = t.getString();
      Unit unit = string.getDelegate();
      if (unit instanceof AssignStmt && ((AssignStmt) unit).getRightOp() instanceof NewExpr) {
        Value leftOp = ((AssignStmt) unit).getLeftOp();
        System.out.println("ALLOCATION SITE" + unit);
        Fact allocFact = new Fact((Local) leftOp);
        WPAutomaton queryAutomaton =
            new WPAutomaton(allocFact, Collections.singleton(new Transition<Stmt, Fact>(allocFact,
                string, Fact.TARGET)), Fact.TARGET);
        WeightedPAutomaton<Stmt, Fact, PDSSet> poststar = pds.poststar(queryAutomaton);

        // System.out.println(poststar);
        SootMethod method = icfg.getMethodOf(stmt);
        for (Unit end : icfg.getEndPointsOf(method)) {
          for (Unit eP : icfg.getPredsOf(end)) {
            for (Transition<Stmt, Fact> trans : poststar.getTransitions()) {
              if (trans.getString().equals(new Stmt(eP)) && trans.getStart().toString().equals("e")) {
                PDSSet weightFor = poststar.getWeightFor(trans);
                AccessStmt s = new AccessStmt(unit);
                FieldPAutomaton fieldauto =
                    new FieldPAutomaton(s,
                        Collections.singleton(new Transition<WrappedSootField, AccessStmt>(s,
                            WrappedSootField.EPSILON, AccessStmt.TARGET)), AccessStmt.TARGET);
                weightFor.printPostStarFor(fieldauto, new AccessStmt(eP));
              }
            }
          }
        }
        System.out.println("END OF ALLOCATION SITE" + unit);
      }
    }
    // TODO Auto-generated method stub

  }
}
