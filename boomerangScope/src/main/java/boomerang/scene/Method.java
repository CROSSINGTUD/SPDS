package boomerang.scene;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import wpds.interfaces.Location;

public abstract class Method implements Location {
  private static Method epsilon;

  protected Method() {}

  public static Method epsilon() {
    if (epsilon == null)
      epsilon =
          new Method() {
            @Override
            public int hashCode() {
              return System.identityHashCode(this);
            }

            @Override
            public boolean equals(Object obj) {
              return obj == this;
            }

            @Override
            public boolean isStaticInitializer() {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public boolean isParameterLocal(Val val) {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public boolean isThisLocal(Val val) {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public Set<Val> getLocals() {
              // TODO Auto-generated method stub
              return Sets.newHashSet();
            }

            @Override
            public Val getThisLocal() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public List<Val> getParameterLocals() {
              // TODO Auto-generated method stub
              return Lists.newArrayList();
            }

            @Override
            public boolean isStatic() {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public boolean isNative() {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public List<Statement> getStatements() {
              // TODO Auto-generated method stub
              return Lists.newArrayList();
            }

            @Override
            public WrappedClass getDeclaringClass() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public ControlFlowGraph getControlFlowGraph() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public String getSubSignature() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public String getName() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public boolean isConstructor() {
              // TODO Auto-generated method stub
              return false;
            }

            @Override
            public boolean isPublic() {
              // TODO Auto-generated method stub
              return false;
            }
          };
    return epsilon;
  }

  @Override
  public String toString() {
    return "METHOD EPS";
  }

  public abstract boolean isStaticInitializer();

  public abstract boolean isParameterLocal(Val val);

  public abstract boolean isThisLocal(Val val);

  public abstract Set<Val> getLocals();

  public abstract Val getThisLocal();

  public abstract List<Val> getParameterLocals();

  public abstract boolean isStatic();

  public abstract boolean isNative();

  public abstract List<Statement> getStatements();

  public abstract WrappedClass getDeclaringClass();

  public abstract ControlFlowGraph getControlFlowGraph();

  public abstract String getSubSignature();

  public abstract String getName();

  public Val getParameterLocal(int i) {
    return getParameterLocals().get(i);
  }

  public abstract boolean isConstructor();

  public abstract boolean isPublic();

  private Collection<Val> returnLocals;

  public Collection<Val> getReturnLocals() {
    if (returnLocals == null) {
      returnLocals = Sets.newHashSet();
      for (Statement s : getStatements()) {
        if (s.isReturnStmt()) {
          returnLocals.add(s.getReturnOp());
        }
      }
    }
    return returnLocals;
  }

  @Override
  public boolean accepts(Location other) {
    return this.equals(other);
  }
}
