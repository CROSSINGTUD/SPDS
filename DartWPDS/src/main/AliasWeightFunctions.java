package main;

import data.AccessStmt;
import data.Fact;
import data.PDSSet;
import data.WrappedSootField;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import wpds.WEdgeFunctions;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;

public class AliasWeightFunctions implements WEdgeFunctions<Unit, Fact, SootMethod, PDSSet> {

  @Override
  public PDSSet getNormalEdgeFunction(Unit curr, Fact currNode, Unit succ, Fact succNode) {

    if (curr instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) curr;
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        if (assignStmt.getRightOp() instanceof Local
            && currNode.equals(new Fact((Local) assignStmt.getRightOp()))) {
          InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
          return new PDSSet(new UPushRule<WrappedSootField, AccessStmt>(new AccessStmt(curr),
              WrappedSootField.ANYFIELD, new AccessStmt(succ), new WrappedSootField(ifr.getField()),
              WrappedSootField.ANYFIELD));
        }
      } else if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
        if (currNode.equals(new Fact((Local) ifr.getBase()))
            && succNode.equals(new Fact((Local) assignStmt.getLeftOp()))) {
          return new PDSSet(new UPopRule<WrappedSootField, AccessStmt>(new AccessStmt(curr),
              new WrappedSootField(ifr.getField()), new AccessStmt(succ)));
        }
      }
    }
    return new PDSSet(new UNormalRule<WrappedSootField, AccessStmt>(new AccessStmt(curr),
        WrappedSootField.ANYFIELD, new AccessStmt(succ), WrappedSootField.ANYFIELD));
  }

  @Override
  public PDSSet getCallEdgeFunction(Unit callStmt, Fact srcNode, Unit calleeStart, Fact destNode) {
    return new PDSSet(new UNormalRule<WrappedSootField, AccessStmt>(new AccessStmt(callStmt),
        WrappedSootField.ANYFIELD, new AccessStmt(calleeStart), WrappedSootField.ANYFIELD));
  }

  @Override
  public PDSSet getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt,
      Fact exitNode, Unit returnSite, Fact retNode) {
    return new PDSSet(new UNormalRule<WrappedSootField, AccessStmt>(new AccessStmt(exitStmt),
        WrappedSootField.ANYFIELD, new AccessStmt(returnSite), WrappedSootField.ANYFIELD));
  }

  @Override
  public PDSSet getCallToReturnEdgeFunction(Unit callSite, Fact callNode, Unit returnSite,
      Fact returnSideNode) {
    return new PDSSet(new UNormalRule<WrappedSootField, AccessStmt>(new AccessStmt(callSite),
        WrappedSootField.ANYFIELD, new AccessStmt(returnSite), WrappedSootField.ANYFIELD));
  }

}
