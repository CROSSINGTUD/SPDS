package data;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.Unit;
import wpds.impl.Rule;
import wpds.impl.Weight;
import wpds.wildcard.Wildcard;
import wpds.wildcard.WildcardPushdownSystem;

public class PDSSet extends Weight<Unit> {
  private final Set<PDS> rules;

  public PDSSet(Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> rule) {
    Set<PDS> outer = new HashSet<PDS>();
    PDS inner = new PDS();
    inner.addRule(rule);
    outer.add(inner);
    rules = outer;
  }

  private PDSSet(Set<PDS> rules) {
    this.rules = rules;
  }

  protected PDSSet() {
    rules = Sets.newHashSet();
  }



  @Override
  public Weight<Unit> extendWith(Weight<Unit> other) {
    if (!(other instanceof PDSSet))
      throw new RuntimeException();
    if (other instanceof One)
      return new PDSSet(rules);
    PDSSet access = (PDSSet) other;
    Set<PDS> outer = deepCopy();
    for (PDS inner : outer) {
      for (PDS otherRules : access.rules) {
        for (Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> otherRule : otherRules
            .getAllRules())
          inner.addRule(otherRule);
      }
    }
    return new PDSSet(outer);
  }

  @Override
  public Weight<Unit> combineWith(Weight<Unit> other) {
    if (!(other instanceof PDSSet))
      throw new RuntimeException();
    PDSSet access = (PDSSet) other;
    Set<PDS> outer = deepCopy();
    for (PDS otherPDS : access.rules) {
      outer.add(otherPDS);
    }
    return new PDSSet(outer);
  }

  private Set<PDS> deepCopy() {
    Set<PDS> result = new HashSet<PDS>();
    for (PDS outer : rules) {
      PDS innerRes = new PDS();
      for (Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> inner : outer
          .getAllRules()) {
        innerRes.addRule(inner);
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
    PDSSet other = (PDSSet) obj;
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

  public class PDS extends WildcardPushdownSystem<WrappedSootField, AccessStmt> {
    @Override
    public String toString() {
      return getAllRules().toString();
    }

    @Override
    public Wildcard anyTransition() {
      return WrappedSootField.ANYFIELD;
    }
  }

  public Set<PDS> getPDSSystems() {
    return rules;
  }

}
