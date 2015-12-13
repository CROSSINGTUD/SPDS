package data;

import java.util.HashSet;
import java.util.Set;

import wpds.impl.Rule;
import wpds.interfaces.Weight;

import com.google.common.collect.Sets;

public class Access implements Weight {
  private final Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> rules;

  public Access(Rule<WrappedSootField, AccessStmt, FieldWeight> rule) {
    Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> outer =
        new HashSet<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>>();
    Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> inner =
        new HashSet<Rule<WrappedSootField, AccessStmt, FieldWeight>>();
    inner.add(rule);
    outer.add(inner);
    rules = outer;
  }

  public Access(Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> rules) {
    this.rules = rules;
  }

  protected Access() {
    rules = Sets.newHashSet();
  }


  @Override
  public Weight extendWith(Weight other) {
    if (!(other instanceof Access))
      throw new RuntimeException();
    if (other instanceof One)
      return new Access(rules);
    Access access = (Access) other;
    Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> outer = deepCopy();
    for (Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> inner : outer) {
      for (Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> otherRules : access.rules) {
        inner.addAll(otherRules);
      }
    }
    return new Access(outer);
  }

  @Override
  public Weight combineWith(Weight other) {
    if (!(other instanceof Access))
      throw new RuntimeException();
    Access access = (Access) other;
    Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> outer = deepCopy();
    for (Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> otherPDS : access.rules) {
      outer.add(otherPDS);
    }
    return new Access(outer);
  }

  private Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> deepCopy() {
    Set<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>> result =
        new HashSet<Set<Rule<WrappedSootField, AccessStmt, FieldWeight>>>();
    for (Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> outer : rules) {
      Set<Rule<WrappedSootField, AccessStmt, FieldWeight>> innerRes =
          new HashSet<Rule<WrappedSootField, AccessStmt, FieldWeight>>();
      for (Rule<WrappedSootField, AccessStmt, FieldWeight> inner : outer) {
        innerRes.add(inner);
      }
      result.add(innerRes);
    }
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rules == null) ? 0 : rules.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Access other = (Access) obj;
    if (rules == null) {
      if (other.rules != null)
        return false;
    } else if (!rules.equals(other.rules))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return rules.toString();
  }
}
