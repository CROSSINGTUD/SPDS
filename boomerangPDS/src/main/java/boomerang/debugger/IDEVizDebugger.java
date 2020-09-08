/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.debugger;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.SuccessorListener;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.solver.ForwardBoomerangSolver;
import boomerang.util.RegExAccessPath;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.NormalRule;
import wpds.impl.Rule;
import wpds.impl.Weight;

public class IDEVizDebugger<W extends Weight> extends Debugger<W> {

  private static boolean ONLY_CFG = false;
  private static final Logger logger = LoggerFactory.getLogger(IDEVizDebugger.class);
  private File ideVizFile;
  private ObservableICFG<Statement, Method> icfg;
  private Table<Query, Method, Set<Rule<Edge, INode<Val>, W>>> rules = HashBasedTable.create();
  private Map<Object, Integer> objectToInteger = new HashMap<>();
  private int charSize;
  private ObservableControlFlowGraph cfg;

  public IDEVizDebugger(File ideVizFile) {
    this.ideVizFile = ideVizFile;
  }

  private void callRules(Query q, Set<Rule<Edge, INode<Val>, W>> allRules) {
    for (Rule<Edge, INode<Val>, W> e : allRules) {
      Edge stmt = e.getL1();
      if (stmt.getMethod() == null) continue;
      Set<Rule<Edge, INode<Val>, W>> transInMethod = getOrCreateRuleSet(q, stmt.getMethod());
      transInMethod.add(e);
    }
  }

  private Set<Rule<Edge, INode<Val>, W>> getOrCreateRuleSet(Query q, Method method) {
    Set<Rule<Edge, INode<Val>, W>> map = rules.get(q, method);
    if (map != null) return map;
    rules.put(q, method, Sets.newHashSet());
    return rules.get(q, method);
  }

  @Override
  public void done(
      ObservableICFG<Statement, Method> icfg,
      ObservableControlFlowGraph confg,
      Set<Method> visitedMethods,
      Map<ForwardQuery, ForwardBoomerangSolver<W>> solvers) {
    this.icfg = icfg;
    this.cfg = confg;
    logger.warn(
        "Starting to compute visualization, this requires a large amount of memory, please ensure the VM has enough memory.");
    Stopwatch watch = Stopwatch.createStarted();
    JSONArray eventualData = new JSONArray();
    if (!ONLY_CFG) {
      for (Query q : solvers.keySet()) {
        callRules(q, solvers.get(q).getCallPDS().getAllRules());
      }
    }
    for (Entry<ForwardQuery, ForwardBoomerangSolver<W>> e :
        Lists.newArrayList(solvers.entrySet())) {
      logger.debug("Computing results for {}", e.getKey());
      Query query = e.getKey();
      JSONQuery queryJSON = new JSONQuery(query);
      JSONArray data = new JSONArray();
      for (Method m : Lists.newArrayList(visitedMethods)) {
        Table<Edge, RegExAccessPath, W> results = e.getValue().getResults(m);
        if (results.isEmpty()) continue;
        int labelYOffset = ONLY_CFG ? 0 : computeLabelYOffset(results.columnKeySet());
        JSONMethod jsonMethod = new JSONMethod(m);
        logger.debug("Creating control-flow graph for {}", m);
        IDEVizDebugger<W>.JSONControlFlowGraph cfg = createControlFlowGraph(m, labelYOffset);

        jsonMethod.put("cfg", cfg);
        if (!ONLY_CFG) {
          Set<Rule<Edge, INode<Val>, W>> rulesInMethod = getOrCreateRuleSet(query, m);
          logger.debug("Creating data-flow graph for {}", m);
          DataFlowGraph dfg =
              createDataFlowGraph(query, results, rulesInMethod, cfg, m, labelYOffset);
          jsonMethod.put("dfg", dfg);
        }
        data.add(jsonMethod);
      }
      queryJSON.put("methods", data);
      eventualData.add(queryJSON);
    }
    ;
    logger.info("Computing visualization took: {}", watch.elapsed());
    try (FileWriter file = new FileWriter(ideVizFile)) {
      logger.info("Writing visualization to file {}", ideVizFile.getAbsolutePath());
      file.write(eventualData.toJSONString());
      logger.info("Visualization available in file {}", ideVizFile.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
      logger.info("Exception in writing to visualization file {}", ideVizFile.getAbsolutePath());
    }
  }

  private int computeLabelYOffset(Set<RegExAccessPath> facts) {
    int labelYOffset = 0;
    for (RegExAccessPath g : facts) {
      labelYOffset = Math.max(labelYOffset, charSize * g.toString().length());
    }
    return labelYOffset;
  }

  private DataFlowGraph createDataFlowGraph(
      Query q,
      Table<Edge, RegExAccessPath, W> table,
      Set<Rule<Edge, INode<Val>, W>> rulesInMethod,
      JSONControlFlowGraph cfg,
      Method m,
      int labelYOffset) {
    LinkedList<RegExAccessPath> factsList = new LinkedList<>();
    DataFlowGraph dataFlowGraph = new DataFlowGraph();
    Set<RegExAccessPath> facts = table.columnKeySet();
    JSONArray data = new JSONArray();

    int offset = 0;
    int charSize = 8;
    for (RegExAccessPath u : facts) {
      JSONObject nodeObj = new JSONObject();
      JSONObject pos = new JSONObject();
      factsList.add(u);
      pos.put("x", factsList.size() * 30 + offset * charSize);
      pos.put("y", labelYOffset);
      nodeObj.put("position", pos);
      JSONObject label = new JSONObject();
      label.put("label", u.toString());
      label.put("factId", id(u));
      nodeObj.put("classes", "fact label method" + id(m));
      nodeObj.put("data", label);
      data.add(nodeObj);
    }

    Multimap<Node<Edge, Val>, RegExAccessPath> esgNodes = HashMultimap.create();
    // System.out.println("Number of nodes:\t" + esg.getNodes().size());
    for (Cell<Edge, RegExAccessPath, W> trans : table.cellSet()) {
      Edge stmt = trans.getRowKey();
      RegExAccessPath val = trans.getColumnKey();
      if (!trans.getRowKey().getMethod().equals(val.getVal().m())) continue;
      JSONObject nodeObj = new JSONObject();
      JSONObject pos = new JSONObject();
      pos.put("x", (factsList.indexOf(val) + 1) * 30 /* + offset * charSize */);
      pos.put(
          "y",
          (cfg.stmtsList.indexOf(stmt)) * 30
              + (q instanceof BackwardQuery ? 30 : 0) /* + labelYOffset */);

      nodeObj.put("position", pos);
      String classes = "esgNode method" + id(m) + " ";

      JSONObject additionalData = new JSONObject();
      additionalData.put("id", "q" + id(q) + "n" + id(new Node<>(stmt, val)));
      additionalData.put("stmtId", id(stmt));
      additionalData.put("factId", id(val));
      if (trans.getValue() != null) additionalData.put("ideValue", trans.getValue().toString());
      nodeObj.put("classes", classes);
      nodeObj.put("group", "nodes");
      nodeObj.put("data", additionalData);

      data.add(nodeObj);

      esgNodes.put(new Node<>(stmt, val.getVal()), val);
    }

    for (Rule<Edge, INode<Val>, W> rule : rulesInMethod) {
      if (!(rule instanceof NormalRule)) {
        continue;
      }
      JSONObject nodeObj = new JSONObject();
      JSONObject dataEntry = new JSONObject();
      dataEntry.put("id", "e" + id(rule));
      Node<Edge, Val> start = getStartNode(rule);
      Node<Edge, Val> target = getTargetNode(rule);
      for (RegExAccessPath startField : esgNodes.get(start)) {
        for (RegExAccessPath targetField : esgNodes.get(target)) {
          dataEntry.put("source", "q" + id(q) + "n" + id(new Node<>(start.stmt(), startField)));
          dataEntry.put("target", "q" + id(q) + "n" + id(new Node<>(target.stmt(), targetField)));
          dataEntry.put("directed", "true");
          dataEntry.put("direction", (q instanceof BackwardQuery ? "Backward" : "Forward"));
          nodeObj.put("data", dataEntry);
          nodeObj.put("classes", "esgEdge  method" + id(m));
          nodeObj.put("group", "edges");
          data.add(nodeObj);
        }
      }
    }
    dataFlowGraph.put("dataFlowNode", data);
    return dataFlowGraph;
  }

  private Node<Edge, Val> getTargetNode(Rule<Edge, INode<Val>, W> rule) {
    return new Node<>(rule.getL2(), rule.getS2().fact());
  }

  private Node<Edge, Val> getStartNode(Rule<Edge, INode<Val>, W> rule) {
    return new Node<>(rule.getL1(), rule.getS1().fact());
  }

  private JSONControlFlowGraph createControlFlowGraph(Method m, int labelYOffset) {
    IDEVizDebugger<W>.JSONControlFlowGraph cfg = new JSONControlFlowGraph();
    int index = 0;
    int offset = 0;
    JSONArray data = new JSONArray();
    for (Statement u : m.getStatements()) {
      if (u.getMethod() == null) {
        continue;
      }
      JSONObject nodeObj = new JSONObject();
      JSONObject pos = new JSONObject();
      cfg.stmtsList.add(u);
      pos.put("x", 10);
      pos.put("y", cfg.stmtsList.size() * 30 + labelYOffset);
      nodeObj.put("position", pos);
      JSONObject label = new JSONObject();
      label.put("label", u.toString());
      label.put("shortLabel", u.toString());
      if (icfg.isCallStmt(u)) {
        label.put("callSite", icfg.isCallStmt(u));
        JSONArray callees = new JSONArray();
        icfg.addCalleeListener(new JsonCalleeListener(u, callees));
        label.put("callees", callees);
      }
      if (icfg.isExitStmt(u)) {
        label.put("returnSite", icfg.isExitStmt(u));
        JSONArray callees = new JSONArray();
        Set<Method> callers = new HashSet<>();
        icfg.addCallerListener(new JsonCallerListener(u, callers));
        for (Method caller : callers) callees.add(new JSONMethod(caller));
        label.put("callers", callees);
      }
      label.put("stmtId", id(u));
      label.put("id", "stmt" + id(u));

      label.put("stmtIndex", index);
      index++;

      nodeObj.put("data", label);
      nodeObj.put(
          "classes",
          "stmt label "
              + (icfg.isExitStmt(u) ? " returnSite " : " ")
              + (icfg.isCallStmt(u) ? " callSite " : " ")
              + " method"
              + id(m));
      data.add(nodeObj);
      offset = Math.max(offset, u.toString().length());

      this.cfg.addSuccsOfListener(
          new SuccessorListener(u) {

            @Override
            public void getSuccessor(Statement succ) {
              JSONObject cfgEdgeObj = new JSONObject();
              JSONObject dataEntry = new JSONObject();
              dataEntry.put("source", "stmt" + id(u));
              dataEntry.put("target", "stmt" + id(succ));
              dataEntry.put("directed", "true");
              cfgEdgeObj.put("data", dataEntry);
              cfgEdgeObj.put("classes", "cfgEdge label method" + id(m));
              data.add(cfgEdgeObj);
            }
          });
    }
    cfg.put("controlFlowNode", data);
    return cfg;
  }

  private class JsonCalleeListener implements CalleeListener<Statement, Method> {
    Statement u;
    JSONArray callees;

    JsonCalleeListener(Statement u, JSONArray callees) {
      this.u = u;
      this.callees = callees;
    }

    @Override
    public Statement getObservedCaller() {
      return u;
    }

    @Override
    public void onCalleeAdded(Statement unit, Method sootMethod) {
      if (sootMethod != null && sootMethod.toString() != null) {
        callees.add(new JSONMethod(sootMethod));
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JsonCalleeListener that = (JsonCalleeListener) o;
      return Objects.equals(u, that.u) && Objects.equals(callees, that.callees);
    }

    @Override
    public int hashCode() {
      return Objects.hash(u, callees);
    }

    @Override
    public void onNoCalleeFound() {}
  }

  private class JsonCallerListener implements CallerListener<Statement, Method> {
    Statement u;
    Set<Method> callers;

    JsonCallerListener(Statement u, Set<Method> callers) {
      this.u = u;
      this.callers = callers;
    }

    @Override
    public Method getObservedCallee() {
      return u.getMethod();
    }

    @Override
    public void onCallerAdded(Statement unit, Method sootMethod) {
      callers.add(unit.getMethod());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JsonCallerListener that = (JsonCallerListener) o;
      return Objects.equals(u, that.u) && Objects.equals(callers, that.callers);
    }

    @Override
    public int hashCode() {

      return Objects.hash(u, callers);
    }
  }

  private class JSONMethod extends JSONObject {

    JSONMethod(Method m) {
      this.put("methodName", m.toString());
      this.put("id", id(m));
    }
  }

  private class JSONQuery extends JSONObject {
    JSONQuery(Query m) {
      this.put("query", prettyPrintQuery(m));
      this.put("id", id(m));
    }

    private String prettyPrintQuery(Query m) {
      return (m instanceof BackwardQuery ? "B " : "F ")
          + m.asNode().fact()
          + " @ "
          + m.asNode().stmt().getMethod();
    }
  }

  private class JSONControlFlowGraph extends JSONObject {
    public List<Statement> stmtsList = Lists.newLinkedList();
  }

  private class DataFlowGraph extends JSONObject {}

  public Integer id(Object u) {
    if (objectToInteger.get(u) != null) return objectToInteger.get(u);
    int size = objectToInteger.size() + 1;
    objectToInteger.put(u, size);
    return size;
  }
}
