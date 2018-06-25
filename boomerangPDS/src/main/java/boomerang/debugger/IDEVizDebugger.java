/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *  
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
package boomerang.debugger;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.CallerListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.solver.AbstractBoomerangSolver;
import boomerang.util.RegExAccessPath;
import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.collect.Table.Cell;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.*;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.NormalRule;
import wpds.impl.Rule;
import wpds.impl.Weight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class IDEVizDebugger<W extends Weight> extends Debugger<W>{

	private static boolean ONLY_CFG = false;
    private static final Logger logger = LogManager.getLogger();
	private File ideVizFile;
	private ObservableICFG<Unit, SootMethod> icfg;
	private Table<Query, SootMethod, Set<Rule<Statement, INode<Val>, W>>> rules = HashBasedTable.create();
	private Map<Object, Integer> objectToInteger = new HashMap<>();
	private int charSize;
	
	
	public IDEVizDebugger(File ideVizFile, ObservableICFG<Unit, SootMethod> icfg) {
		this.ideVizFile = ideVizFile;
		this.icfg = icfg;
	}

	
	private void callRules(Query q, Set<Rule<Statement, INode<Val>, W>> allRules) {
		for(Rule<Statement, INode<Val>, W> e : allRules){
			Statement stmt = e.getL1();
			if(stmt.getMethod() == null)
				continue;
			Set<Rule<Statement, INode<Val>, W>> transInMethod = getOrCreateRuleSet(q,stmt.getMethod());
			transInMethod.add(e);
		}
	}
	

	private Set<Rule<Statement, INode<Val>, W>>  getOrCreateRuleSet(Query q, SootMethod method) {
		Set<Rule<Statement, INode<Val>, W>> map = rules.get(q, method);  
		if(map != null)
			return map;
		rules.put(q, method, Sets.<Rule<Statement, INode<Val>, W>>newHashSet());
		return rules.get(q, method);
	}

	@Override
	public void done(Map<Query, AbstractBoomerangSolver<W>> solvers){
		logger.warn("Starting to compute visualization, this requires a large amount of memory, please ensure the VM has enough memory.");
		Stopwatch watch = Stopwatch.createStarted();
		JSONArray eventualData = new JSONArray();
		if(!ONLY_CFG) {
			for (Query q : solvers.keySet()) {
				callRules(q, solvers.get(q).getCallPDS().getAllRules());
			}
		}
		for(Entry<Query, AbstractBoomerangSolver<W>> e : solvers.entrySet()){
			logger.debug("Computing results for {}",e.getKey());
			Query query = e.getKey();
			JSONQuery queryJSON = new JSONQuery(query);
			JSONArray data = new JSONArray();
			for(SootMethod m : e.getValue().getReachableMethods()) {
				Table<Statement, RegExAccessPath, W> results = e.getValue().getResults(m);
				if(results.isEmpty())
					continue;
				int labelYOffset = ONLY_CFG ? 0 : computeLabelYOffset(results.columnKeySet());
				JSONMethod jsonMethod = new JSONMethod(m);
				logger.debug("Creating control-flow graph for {}",m);
				IDEVizDebugger<W>.JSONControlFlowGraph cfg = createControlFlowGraph(m, labelYOffset);
				
				jsonMethod.put("cfg", cfg);
				if(!ONLY_CFG) {
					Set<Rule<Statement, INode<Val>, W>> rulesInMethod = getOrCreateRuleSet(query,m);
					logger.debug("Creating data-flow graph for {}",m);
					DataFlowGraph dfg = createDataFlowGraph(query, results,rulesInMethod,cfg,m,labelYOffset);
					jsonMethod.put("dfg", dfg);
				}
				data.add(jsonMethod);
			}
			queryJSON.put("methods",data);
			eventualData.add(queryJSON);
		};
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

	private DataFlowGraph createDataFlowGraph(Query q, Table<Statement, RegExAccessPath, W> table,
			Set<Rule<Statement, INode<Val>, W>> rulesInMethod, JSONControlFlowGraph cfg, SootMethod m, int labelYOffset) {
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

		Multimap<Node<Statement,Val>, RegExAccessPath> esgNodes = HashMultimap.create();
		// System.out.println("Number of nodes:\t" + esg.getNodes().size());
		for (Cell<Statement, RegExAccessPath, W> trans : table.cellSet()) {
			Statement statement = trans.getRowKey();
			Stmt stmt = statement.getUnit().get();
			RegExAccessPath val = trans.getColumnKey();
			if(!trans.getRowKey().getMethod().equals(val.getVal().m()))
				continue;
			JSONObject nodeObj = new JSONObject();
			JSONObject pos = new JSONObject();
			pos.put("x", (factsList.indexOf(val) + 1) * 30 /*+ offset * charSize*/);
			pos.put("y",
					(cfg.stmtsList.indexOf(stmt)) * 30 + (q instanceof BackwardQuery ?  30 : 0) /*+ labelYOffset*/);

			nodeObj.put("position", pos);
			String classes = "esgNode method" + id(m) + " ";

			JSONObject additionalData = new JSONObject();
			additionalData.put("id", "q"+id(q)+"n" + id(new Node<Statement,RegExAccessPath>(statement,val)));
			additionalData.put("stmtId", id(stmt));
			additionalData.put("factId", id(val));
			if (trans.getValue() != null)
				additionalData.put("ideValue", trans.getValue().toString());
			nodeObj.put("classes", classes);
			nodeObj.put("group", "nodes");
			nodeObj.put("data", additionalData);

			data.add(nodeObj);

			esgNodes.put(new Node<Statement,Val>(statement,val.getVal()), val);
		}

		for (Rule<Statement, INode<Val>, W> rule : rulesInMethod) {
			if(!(rule instanceof NormalRule)){
				continue;
			}
			JSONObject nodeObj = new JSONObject();
			JSONObject dataEntry = new JSONObject();
			dataEntry.put("id", "e" + id(rule));
			Node<Statement,Val> start = getStartNode(rule);
			Node<Statement,Val> target = getTargetNode(rule);
			for(RegExAccessPath startField: esgNodes.get(start)) {
				for(RegExAccessPath targetField: esgNodes.get(target)) {
					dataEntry.put("source", "q"+id(q)+ "n" + id(new Node<Statement,RegExAccessPath>(start.stmt(),startField)));
					dataEntry.put("target",  "q"+id(q)+"n" + id(new Node<Statement,RegExAccessPath>(target.stmt(),targetField)));
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

	private Node<Statement,Val> getTargetNode(Rule<Statement, INode<Val>, W> rule) {
		return new Node<Statement,Val>(rule.getL2(),rule.getS2().fact());
	}


	private Node<Statement,Val> getStartNode(Rule<Statement, INode<Val>, W> rule) {
		return new Node<Statement,Val>(rule.getL1(),rule.getS1().fact());
	}

	private JSONControlFlowGraph createControlFlowGraph(SootMethod m, int labelYOffset){
		IDEVizDebugger<W>.JSONControlFlowGraph cfg = new JSONControlFlowGraph();
		int index = 0;	
		int offset = 0;
		JSONArray data = new JSONArray();
		for (Unit u : m.getActiveBody().getUnits()) {
			if(icfg.getMethodOf(u) == null) {
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
			label.put("shortLabel", getShortLabel(u));
			if (icfg.isCallStmt(u)) {
				label.put("callSite", icfg.isCallStmt(u));
				JSONArray callees = new JSONArray();
				icfg.addCalleeListener(new CalleeListener<Unit, SootMethod>(){

					@Override
					public Unit getObservedCaller() {
						return u;
					}

					@Override
					public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
						if (sootMethod != null && sootMethod.toString() != null){
							callees.add(new JSONMethod(sootMethod));
						}
					}
				});
				label.put("callees", callees);
			}
			if (icfg.isExitStmt(u)) {
				label.put("returnSite", icfg.isExitStmt(u));
				JSONArray callees = new JSONArray();
				Set<SootMethod> callers = new HashSet<>();
				SootMethod callingMethod = icfg.getMethodOf(u);
				icfg.addCallerListener(new CallerListener<Unit,SootMethod>(){

					@Override
					public SootMethod getObservedCallee() {
						return callingMethod;
					}

					@Override
					public void onCallerAdded(Unit unit, SootMethod sootMethod) {
						callers.add(callingMethod);
					}
				});
	
				for (SootMethod caller : callers)
					callees.add(new JSONMethod(caller));
				label.put("callers", callees);
			}
			label.put("stmtId", id(u));
			label.put("id", "stmt" + id(u));
	
			label.put("stmtIndex", index);
			index++;
	
			nodeObj.put("data", label);
			nodeObj.put("classes", "stmt label " + (icfg.isExitStmt(u) ? " returnSite " : " ")
					+ (icfg.isCallStmt(u) ? " callSite " : " ") + " method" + id(m));
			data.add(nodeObj);
			offset = Math.max(offset, getShortLabel(u).toString().length());
	
			for (Unit succ : icfg.getSuccsOf(u)) {
				JSONObject cfgEdgeObj = new JSONObject();
				JSONObject dataEntry = new JSONObject();
				dataEntry.put("source", "stmt" + id(u));
				dataEntry.put("target", "stmt" + id(succ));
				dataEntry.put("directed", "true");
				cfgEdgeObj.put("data", dataEntry);
				cfgEdgeObj.put("classes", "cfgEdge label method" + id(m));
				data.add(cfgEdgeObj);
			}
		}
		cfg.put("controlFlowNode", data);
		return cfg;
	}

	private String getShortLabel(Unit u) {
		if (u instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) u;
			if (assignStmt.getRightOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getRightOp();
				return assignStmt.getLeftOp() + " = " + fr.getBase() + "." + fr.getField().getName();
			}
			if (assignStmt.getLeftOp() instanceof InstanceFieldRef) {
				InstanceFieldRef fr = (InstanceFieldRef) assignStmt.getLeftOp();
				return fr.getBase() + "." + fr.getField().getName() + " = " + assignStmt.getRightOp();
			}
		}
		if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
			InvokeExpr invokeExpr = ((Stmt) u).getInvokeExpr();
			if (invokeExpr instanceof StaticInvokeExpr)
				return (u instanceof AssignStmt ? ((AssignStmt) u).getLeftOp() + " = " : "")
						+ invokeExpr.getMethod().getName() + "("
						+ invokeExpr.getArgs().toString().replace("[", "").replace("]", "") + ")";
			if (invokeExpr instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
				return (u instanceof AssignStmt ? ((AssignStmt) u).getLeftOp() + " = " : "") + iie.getBase() + "."
						+ invokeExpr.getMethod().getName() + "("
						+ invokeExpr.getArgs().toString().replace("[", "").replace("]", "") + ")";
			}
		}
		return u.toString();
	}

	private class JSONMethod extends JSONObject {

		JSONMethod(SootMethod m) {
			this.put("methodName", StringEscapeUtils.escapeHtml4(m.toString()));
			this.put("id", id(m));
		}
	}
	
	private class JSONQuery extends JSONObject {
		JSONQuery(Query m) {
			this.put("query", StringEscapeUtils.escapeHtml4(prettyPrintQuery(m)));
			this.put("id", id(m));
		}

		private String prettyPrintQuery(Query m) {
			return (m instanceof BackwardQuery ? "B " : "F ") + m.asNode().fact().value() + " @ " + m.asNode().stmt().getMethod().getName();
		}
	}
	private class JSONControlFlowGraph extends JSONObject {
		public List<Unit> stmtsList = Lists.newLinkedList() ;
	}
	private class DataFlowGraph extends JSONObject {
	}
	
	

	public Integer id(Object u) {
		if (objectToInteger.get(u) != null)
			return objectToInteger.get(u);
		int size = objectToInteger.size() + 1;
		objectToInteger.put(u, size);
		return size;
	}
	
}
