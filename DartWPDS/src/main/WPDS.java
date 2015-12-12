package main;

import soot.jimple.Jimple;
import wpds.impl.PushdownSystem;
import data.Access;
import data.Fact;
import data.One;
import data.Stmt;
import data.Zero;

public class WPDS extends PushdownSystem<Stmt, Fact, Access> {

  @Override
  public Access getZero() {
    return Zero.v();
  }

  @Override
  public Access getOne() {
    return One.v();
  }

  Stmt epsilon;

  @Override
  public Stmt epsilon() {
    if (epsilon == null)
      epsilon = new Stmt(Jimple.v().newNopStmt());
    return epsilon;
  }

}
