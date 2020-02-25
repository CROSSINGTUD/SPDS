package boomerang.scene.jimple;

import boomerang.scene.AllocVal;
import boomerang.scene.CallSiteStatement;
import boomerang.scene.Method;
import boomerang.scene.ReturnSiteStatement;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.NopStmt;
import sync.pds.solver.nodes.Node;

public class ResultStringifier {
  public static final String CLASS = "Class";
  public static final String METHOD = "Method";
  public static final String STATEMENT_INDEX = "StatementIndex";
  public static final String VARIABLE_INDEX = "VariableIndex";
  public static ResultStringifier instance;
  private Map<Node<Statement, Val>, Map<String, String>> nodeCache = Maps.newHashMap();
  private Map<Statement, Map<String, String>> statementCache = Maps.newHashMap();

  public static ResultStringifier v() {
    if (instance == null) instance = new ResultStringifier();
    return instance;
  }

  public static void reset() {
    instance = null;
  }

  public Map<String, String> fromNode(Node<Statement, Val> node) {
    if (nodeCache.containsKey(node)) {
      return nodeCache.get(node);
    }
    Map<String, String> res = fromStatement(node.stmt());
    Statement s = node.stmt();
    if (s instanceof CallSiteStatement) {
      s = ((CallSiteStatement) s).getDelegate();
    } else if (s instanceof ReturnSiteStatement) {
      s = ((ReturnSiteStatement) s).getDelegate();
    }
    Val val = node.fact();
    if (val instanceof AllocVal) {
      val = ((AllocVal) val).getDelegate();
    }
    if (s instanceof JimpleStatement && val instanceof JimpleVal) {
      JimpleStatement jimpleStatement = (JimpleStatement) s;
      JimpleVal jimpleVal = (JimpleVal) val;
      Method method = s.getMethod();
      JimpleMethod m = ((JimpleMethod) method);
      SootMethod delegate = m.getDelegate();
      if (jimpleStatement.getDelegate() instanceof NopStmt) {
        res.put(VARIABLE_INDEX, Integer.toString(-1));
        nodeCache.put(node, res);
        return res;
      }
      for (Unit u : delegate.getActiveBody().getUnits()) {
        if (u.equals(jimpleStatement.getDelegate())) {
          int index = 0;
          List<ValueBox> useAndDefBoxes = u.getUseAndDefBoxes();
          for (ValueBox vb : useAndDefBoxes) {
            if (vb.getValue().equals(jimpleVal.getDelegate())) {
              res.put(VARIABLE_INDEX, Integer.toString(index));
              nodeCache.put(node, res);
              return res;
            }
            index++;
          }
          throw new RuntimeException(
              "Value " + val + " not used in " + jimpleStatement.getDelegate());
        }
      }
      throw new RuntimeException(
          "Unit not found "
              + jimpleStatement.getDelegate()
              + " in body \n "
              + delegate.getActiveBody());
    }
    throw new RuntimeException("Not yet implemented for type " + s.getClass());
  }

  public Map<String, String> fromStatement(Statement s) {
    if (statementCache.containsKey(s)) {
      return statementCache.get(s);
    }
    Map<String, String> result = Maps.newHashMap();
    if (s instanceof CallSiteStatement) {
      s = ((CallSiteStatement) s).getDelegate();
    } else if (s instanceof ReturnSiteStatement) {
      s = ((ReturnSiteStatement) s).getDelegate();
    }
    if (s instanceof JimpleStatement) {
      JimpleStatement jimpleStatement = (JimpleStatement) s;
      Method method = s.getMethod();
      JimpleMethod m = ((JimpleMethod) method);
      SootMethod delegate = m.getDelegate();
      result.put(CLASS, s.getMethod().getDeclaringClass().getFullyQualifiedName());
      result.put(METHOD, delegate.getSignature());
      int index = 0;
      for (Unit u : delegate.getActiveBody().getUnits()) {
        if (u.equals(jimpleStatement.getDelegate())) {
          result.put(STATEMENT_INDEX, Integer.toString(index));
          statementCache.put(s, result);
          return result;
        }
        index++;
      }
      throw new RuntimeException(
          "Unit not found "
              + jimpleStatement.getDelegate()
              + " in body \n "
              + delegate.getActiveBody());
    }
    throw new RuntimeException("Not yet implemented for type " + s.getClass());
  }
}
