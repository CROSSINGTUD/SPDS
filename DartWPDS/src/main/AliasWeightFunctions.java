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
import wpds.impl.NormalRule;
import wpds.impl.PopRule;
import wpds.impl.PushRule;
import wpds.interfaces.Weight;
import wpds.interfaces.Weight.NoWeight;

public class AliasWeightFunctions implements WEdgeFunctions<Unit, Fact, SootMethod, PDSSet> {

  @Override
  public PDSSet getNormalEdgeFunction(Unit curr, Fact currNode, Unit succ, Fact succNode) {
    if (curr instanceof AssignStmt) {
      AssignStmt assignStmt = (AssignStmt) curr;
      if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
        if (assignStmt.getRightOp() instanceof Local
            && currNode.equals(new Fact((Local) assignStmt.getRightOp()))) {
          InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getLeftOp();
          return new PDSSet(new PushRule<WrappedSootField, AccessStmt, NoWeight>(
              WrappedSootField.ANYFIELD, new AccessStmt(curr), WrappedSootField.ANYFIELD,
              new WrappedSootField(ifr.getField()), new AccessStmt(succ), Weight.NO_WEIGHT));
        }
      } else if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
        InstanceFieldRef ifr = (InstanceFieldRef) assignStmt.getRightOp();
        if (currNode.equals(new Fact((Local) ifr.getBase()))) {
          return new PDSSet(new PopRule<WrappedSootField, AccessStmt, NoWeight>(
              new WrappedSootField(ifr.getField()), new AccessStmt(curr), new AccessStmt(succ),
              Weight.NO_WEIGHT));
        }
      }
    }
    return new PDSSet(new NormalRule<WrappedSootField, AccessStmt, NoWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(curr), WrappedSootField.ANYFIELD,
        new AccessStmt(succ), Weight.NO_WEIGHT));
  }

  @Override
  public PDSSet getCallEdgeFunction(Unit callStmt, Fact srcNode, Unit calleeStart,
      Fact destNode) {
    return new PDSSet(new NormalRule<WrappedSootField, AccessStmt, NoWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(callStmt), WrappedSootField.ANYFIELD,
        new AccessStmt(calleeStart), Weight.NO_WEIGHT));
  }

  @Override
  public PDSSet getReturnEdgeFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt,
      Fact exitNode, Unit returnSite, Fact retNode) {
    return new PDSSet(new NormalRule<WrappedSootField, AccessStmt, NoWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(exitStmt), WrappedSootField.ANYFIELD,
        new AccessStmt(returnSite), Weight.NO_WEIGHT));
  }

  @Override
  public PDSSet getCallToReturnEdgeFunction(Unit callSite, Fact callNode, Unit returnSite,
      Fact returnSideNode) {
      return new PDSSet(new NormalRule<WrappedSootField, AccessStmt, NoWeight>(
        WrappedSootField.ANYFIELD, new AccessStmt(callSite), WrappedSootField.ANYFIELD,
          new AccessStmt(returnSite), Weight.NO_WEIGHT));
  }

}
