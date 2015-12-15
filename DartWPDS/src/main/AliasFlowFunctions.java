package main;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import data.Fact;
import heros.FlowFunction;
import heros.FlowFunctions;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;

public class AliasFlowFunctions implements FlowFunctions<Unit, Fact, SootMethod> {

  @Override
  public FlowFunction<Fact> getNormalFlowFunction(final Unit curr, final Unit succ) {
    return new FlowFunction<Fact>() {
      @Override
      public Set<Fact> computeTargets(Fact source) {
        if (curr instanceof AssignStmt) {
          AssignStmt assign = (AssignStmt) curr;

          if (assign.getLeftOp() instanceof Local
              && source.equals(new Fact((Local) assign.getLeftOp())))
            return Collections.emptySet();

          if (assign.getRightOp() instanceof NewExpr) {
            if (source.equals(Fact.REACHABLE)) {
              Fact fact = new Fact((Local) assign.getLeftOp());
              return Collections.singleton(fact);
            }
          } else if (assign.getRightOp() instanceof Local) {
            if (source.equals(new Fact((Local) assign.getRightOp()))) {
              if (assign.getLeftOp() instanceof Local) {
                Local leftOp = (Local) assign.getLeftOp();
                Set<Fact> out = new HashSet<>();
                out.add(source);
                out.add(new Fact(leftOp));
                return out;
              } else if (assign.getLeftOp() instanceof InstanceFieldRef) {
                InstanceFieldRef leftOp = (InstanceFieldRef) assign.getLeftOp();
                Set<Fact> out = new HashSet<>();
                out.add(source);
                out.add(new Fact((Local) leftOp.getBase()));
                return out;
              }
            }
          } else if (assign.getRightOp() instanceof InstanceFieldRef) {
            InstanceFieldRef instanceFieldRef = (InstanceFieldRef) assign.getRightOp();
            if (source.equals(new Fact((Local) instanceFieldRef.getBase()))) {
              if (assign.getLeftOp() instanceof Local) {
                Local leftOp = (Local) assign.getLeftOp();
                Set<Fact> out = new HashSet<>();
                out.add(source);
                out.add(new Fact(leftOp));
                return out;
              }
            }
          }
        }
        return Collections.singleton(source);
      }
    };
  }

  @Override
  public FlowFunction<Fact> getCallFlowFunction(Unit callStmt, final SootMethod callee) {
    final InvokeExpr vie = ((Stmt) callStmt).getInvokeExpr();
    final Local[] paramLocals = new Local[vie.getArgCount()];
    for (int i = 0; i < vie.getArgCount(); i++) {
      paramLocals[i] = (Local) vie.getArg(i);
    }
    return new FlowFunction<Fact>() {

      @Override
      public Set<Fact> computeTargets(Fact source) {
        if (vie instanceof InstanceInvokeExpr) {
          InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) vie;
          if (source.equals(new Fact((Local) instanceInvokeExpr.getBase()))) {
            return Collections.singleton(new Fact(callee.getActiveBody().getThisLocal()));
          }
        }
        if (paramLocals != null)
          for (int i = 0; i < paramLocals.length; i++) {
            if (source.equals(new Fact(paramLocals[i]))) {
              return Collections.singleton(new Fact(callee.getActiveBody().getParameterLocal(i)));
            }
          }
        return Collections.emptySet();
      }

    };
  }


  @Override
  public FlowFunction<Fact> getReturnFlowFunction(final Unit callSite, final SootMethod callee,
      Unit exitStmt, Unit returnSite) {
    return new FlowFunction<Fact>(){
      @Override
      public Set<Fact> computeTargets(Fact source) {
        Stmt inv = (Stmt) callSite;
        if (!callee.isStatic()
            && source.equals(new Fact(callee.getActiveBody().getThisLocal()))) {
          return Collections
              .singleton(new Fact((Local) ((InstanceInvokeExpr) inv.getInvokeExpr()).getBase()));
        }
        Local[] paramLocals = new Local[callee.getActiveBody().getParameterLocals().size()];
        for (int i = 0; i < paramLocals.length; i++) {
          if(new Fact(callee.getActiveBody().getParameterLocal(i)).equals(source)){
            return Collections.singleton(new Fact((Local) inv.getInvokeExpr().getArg(i)));
          }
        }
        return Collections.emptySet();
      }
    };
  }

  @Override
  public FlowFunction<Fact> getCallToReturnFlowFunction(final Unit callSite, Unit returnSite) {
    return new FlowFunction<Fact>() {
      @Override
      public Set<Fact> computeTargets(Fact source) {
        if(callSite instanceof AssignStmt){
          if (source.equals(new Fact((Local) ((AssignStmt) callSite).getLeftOp()))) {
            return Collections.emptySet();
          }
        }
        return Collections.singleton(source);
      }
    };
  }

}
