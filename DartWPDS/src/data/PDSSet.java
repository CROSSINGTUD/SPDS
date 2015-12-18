package data;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.google.common.collect.Sets;

import soot.Unit;
import wpds.impl.Rule;
import wpds.impl.UNormalRule;
import wpds.impl.Weight;
import wpds.wildcard.Wildcard;
import wpds.wildcard.WildcardPushdownSystem;

public class PDSSet extends Weight<Unit> {
  private final Set<AccessStack> rules;


  public PDSSet(Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> rule) {
    Set<AccessStack> outer =
        new HashSet<>();
    AccessStack inner = new AccessStack();
    inner.add(rule);
    outer.add(inner);
    rules = outer;
  }

  private PDSSet(Set<AccessStack> rules) {
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
    Set<AccessStack> outer = deepCopy();
    for (AccessStack inner : outer) {
      for (AccessStack otherRules : access.rules) {
        for (Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> otherRule : otherRules)
          if (!inner.contains(otherRule)) {
            inner.add(otherRule);
          } else {
            Rule<WrappedSootField, AccessStmt, wpds.impl.Weight.NoWeight<WrappedSootField>> last =
                inner.getLast();
            AccessStmt s1 = last.getS2();
            UNormalRule<WrappedSootField, AccessStmt> loopRule = new UNormalRule<WrappedSootField, AccessStmt>(s1, WrappedSootField.ANYFIELD,
                    otherRule.getS1(), WrappedSootField.ANYFIELD);
            if(!inner.contains(loopRule)){
              inner.add(new UNormalRule<WrappedSootField, AccessStmt>(s1, WrappedSootField.ANYFIELD,
                  otherRule.getS1(), WrappedSootField.ANYFIELD));
            }
          }
      }
    }
    return new PDSSet(outer);
  }

  @Override
  public Weight<Unit> combineWith(Weight<Unit> other) {
    if (!(other instanceof PDSSet))
      throw new RuntimeException();
    PDSSet access = (PDSSet) other;
    Set<AccessStack> outer = deepCopy();
    for (AccessStack otherPDS : access.rules) {
      outer.add(otherPDS);
    }
    return new PDSSet(outer);
  }

  private Set<AccessStack> deepCopy() {
    Set<AccessStack> result = new HashSet<AccessStack>();
    for (AccessStack outer : rules) {
      AccessStack innerRes = new AccessStack();
      for (Rule<WrappedSootField, AccessStmt, wpds.impl.Weight.NoWeight<WrappedSootField>> inner : outer) {
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
    private AccessStmt last;
    @Override
    public String toString() {
      return getAllRules().toString();
    }

    @Override
    public Wildcard anyTransition() {
      return WrappedSootField.ANYFIELD;
    }

    public AccessStmt getLastStmt() {
      return last;
    }
  }

  public Set<PDS> getPDSSystems() {

    return converToPDS();
  }

  private Set<PDS> converToPDS() {
    Set<PDS> result = Sets.newHashSet();
    for (AccessStack as : rules) {
      result.add(convertToPDS(as));
    }
    return result;
  }

  private PDS convertToPDS(AccessStack as) {
    PDS pds = new PDS();
    AccessStmt last = null;
    System.out.println(as);
    for (Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>> rule : as) {
      if (last != null) {
        rule.setS1(last);
      }
      pds.addRule(rule);
      last = rule.getS2();
    }
    pds.last = last;
    return pds;
  }

  private class AccessStack
      extends LinkedList<Rule<WrappedSootField, AccessStmt, NoWeight<WrappedSootField>>> {
  }

}
