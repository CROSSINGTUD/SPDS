package main;

import wpds.impl.PushdownSystem;
import data.Fact;
import data.One;
import data.PDSSet;
import data.Stmt;
import data.Zero;

public class WPDS extends PushdownSystem<Stmt, Fact, PDSSet> {

  @Override
  public PDSSet getZero() {
    return Zero.v();
  }

  @Override
  public PDSSet getOne() {
    return One.v();
  }



  @Override
  public Stmt anyTransition() {
    return null;
  }

}
