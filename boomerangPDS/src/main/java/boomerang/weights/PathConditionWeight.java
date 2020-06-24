package boomerang.weights;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import wpds.impl.Weight;

public class PathConditionWeight extends Weight {

  private static PathConditionWeight one;
  private Map<Statement, ConditionDomain> ifStatements = Maps.newHashMap();
  private Map<Val, ConditionDomain> variableToValue = Maps.newHashMap();
  private Set<Val> returnVals = Sets.newHashSet();
  private Map<Method, Statement> calleeToCallSite = Maps.newHashMap();
  private String rep;

  private PathConditionWeight(String rep) {
    this.rep = rep;
  }

  public PathConditionWeight(Statement callSite, Method callee) {
    this.calleeToCallSite.put(callee, callSite);
  }

  public static PathConditionWeight one() {
    if (one == null) {
      one = new PathConditionWeight("ONE");
    }
    return one;
  }

  public PathConditionWeight(Val returnVal) {
    this.returnVals.add(returnVal);
  }

  private PathConditionWeight(
      Map<Statement, ConditionDomain> ifStatements,
      Map<Val, ConditionDomain> variableToValue,
      Set<Val> returnVals,
      Map<Method, Statement> calleeToCallSiteMapping) {
    this.ifStatements = ifStatements;
    this.variableToValue = variableToValue;
    this.returnVals = returnVals;
    this.calleeToCallSite = calleeToCallSiteMapping;
  }

  public PathConditionWeight(Statement ifStatement, Boolean condition) {
    ifStatements.put(ifStatement, condition ? ConditionDomain.TRUE : ConditionDomain.FALSE);
  }

  public PathConditionWeight(Val val, ConditionDomain c) {
    variableToValue.put(val, c);
  }

  public enum ConditionDomain {
    TRUE,
    FALSE,
    TOP
  }

  @Override
  public Weight extendWith(Weight o) {
    if (!(o instanceof PathConditionWeight)) {
      throw new RuntimeException("Cannot extend to different types of weight!");
    }
    PathConditionWeight other = (PathConditionWeight) o;
    Map<Statement, ConditionDomain> newIfs = Maps.newHashMap();

    newIfs.putAll(ifStatements);
    for (Map.Entry<Statement, ConditionDomain> e : other.ifStatements.entrySet()) {
      if (newIfs.containsKey(e.getKey()) && e.getValue().equals(ConditionDomain.TOP)) {
        newIfs.put(e.getKey(), ConditionDomain.TOP);
      } else {
        newIfs.put(e.getKey(), e.getValue());
      }
    }

    Map<Val, ConditionDomain> newVals = Maps.newHashMap();

    newVals.putAll(variableToValue);
    for (Map.Entry<Val, ConditionDomain> e : other.variableToValue.entrySet()) {
      if (newVals.containsKey(e.getKey()) && e.getValue().equals(ConditionDomain.TOP)) {
        newVals.put(e.getKey(), ConditionDomain.TOP);
      } else {
        newVals.put(e.getKey(), e.getValue());
      }
    }

    // May become a performance bottleneck
    Map<Val, ConditionDomain> returnToAssignedVariableMap = Maps.newHashMap();
    if (!returnVals.isEmpty()) {
      for (Map.Entry<Val, ConditionDomain> v : newVals.entrySet()) {
        if (returnVals.contains(v.getKey())) {
          Statement s = calleeToCallSite.get(v.getKey().m());
          if (s != null) {
            Val leftOp = s.getLeftOp();
            returnToAssignedVariableMap.put(leftOp, v.getValue());
          }
        }
      }
    }
    newVals.putAll(returnToAssignedVariableMap);
    Set<Val> newReturnVals = Sets.newHashSet(returnVals);
    newReturnVals.addAll(other.returnVals);
    Map<Method, Statement> calleeToCallSiteMapping = Maps.newHashMap(calleeToCallSite);
    calleeToCallSiteMapping.putAll(other.calleeToCallSite);
    return new PathConditionWeight(newIfs, newVals, newReturnVals, calleeToCallSiteMapping);
  }

  @Override
  public Weight combineWith(Weight o) {
    if (!(o instanceof PathConditionWeight)) {
      throw new RuntimeException("Cannot extend to different types of weight!");
    }
    PathConditionWeight other = (PathConditionWeight) o;
    Map<Statement, ConditionDomain> newIfs = Maps.newHashMap();
    for (Map.Entry<Statement, ConditionDomain> e : ifStatements.entrySet()) {
      if (other.ifStatements.containsKey(e.getKey())) {
        ConditionDomain otherVal = other.ifStatements.get(e.getKey());
        if (e.getValue().equals(otherVal)) {
          newIfs.put(e.getKey(), otherVal);
        } else {
          newIfs.put(e.getKey(), ConditionDomain.TOP);
        }
      } else {
        newIfs.put(e.getKey(), e.getValue());
      }
    }
    for (Map.Entry<Statement, ConditionDomain> e : other.ifStatements.entrySet()) {
      if (!ifStatements.containsKey(e.getKey())) {
        newIfs.put(e.getKey(), e.getValue());
      }
    }

    Map<Val, ConditionDomain> newVals = Maps.newHashMap();
    for (Map.Entry<Val, ConditionDomain> e : variableToValue.entrySet()) {
      if (other.variableToValue.containsKey(e.getKey())) {
        ConditionDomain otherVal = other.variableToValue.get(e.getKey());
        if (e.getValue().equals(otherVal)) {
          newVals.put(e.getKey(), otherVal);
        } else {
          newVals.put(e.getKey(), ConditionDomain.TOP);
        }
      } else {
        newVals.put(e.getKey(), e.getValue());
      }
    }
    for (Map.Entry<Val, ConditionDomain> e : other.variableToValue.entrySet()) {
      if (!variableToValue.containsKey(e.getKey())) {
        newVals.put(e.getKey(), e.getValue());
      }
    }

    Map<Val, ConditionDomain> returnToAssignedVariableMap = Maps.newHashMap();
    if (!returnVals.isEmpty()) {
      for (Map.Entry<Val, ConditionDomain> v : newVals.entrySet()) {
        if (returnVals.contains(v.getKey())) {
          Statement s = calleeToCallSite.get(v.getKey().m());
          if (s != null) {
            Val leftOp = s.getLeftOp();
            returnToAssignedVariableMap.put(leftOp, v.getValue());
          }
        }
      }
    }
    newVals.putAll(returnToAssignedVariableMap);
    Set<Val> newReturnVals = Sets.newHashSet(returnVals);
    newReturnVals.addAll(other.returnVals);
    Map<Method, Statement> calleeToCallSiteMapping = Maps.newHashMap(calleeToCallSite);
    calleeToCallSiteMapping.putAll(other.calleeToCallSite);
    return new PathConditionWeight(newIfs, newVals, newReturnVals, calleeToCallSiteMapping);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ifStatements == null) ? 0 : ifStatements.hashCode());
    result = prime * result + ((variableToValue == null) ? 0 : variableToValue.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    PathConditionWeight other = (PathConditionWeight) obj;
    if (ifStatements == null) {
      if (other.ifStatements != null) {
        return false;
      }
    } else if (!ifStatements.equals(other.ifStatements)) {
      return false;
    }

    if (variableToValue == null) {
      if (other.variableToValue != null) {
        return false;
      }
    } else if (!variableToValue.equals(other.variableToValue)) {
      return false;
    }
    if (rep == null) {
      if (other.rep != null) {
        return false;
      }
    } else if (!rep.equals(other.rep)) {
      return false;
    }
    return true;
  }

  public Map<Statement, ConditionDomain> getConditions() {
    return ifStatements;
  }

  public Map<Val, ConditionDomain> getEvaluationMap() {
    return variableToValue;
  }

  @Override
  public String toString() {
    return "\nIf statements: " + ifStatements + " Vals: " + variableToValue;
  }
}
