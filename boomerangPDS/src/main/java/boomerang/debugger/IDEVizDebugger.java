package boomerang.debugger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import heros.utilities.DefaultValueMap;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.NormalRule;
import wpds.impl.Rule;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class IDEVizDebugger<W extends Weight> extends Debugger<W>{

	private File ideVizFile;
	private InterproceduralCFG<Unit, SootMethod> icfg;
	private Table<Query, SootMethod, Map<Transition<Statement, INode<Val>>, W>> reachedNodes = HashBasedTable.create();
	private Table<Query, SootMethod, Set<Rule<Statement, INode<Val>, W>>> rules = HashBasedTable.create();
	private Map<Object, Integer> objectToInteger = new HashMap<>();
	private int charSize;
	
	public IDEVizDebugger(File ideVizFile, InterproceduralCFG<Unit, SootMethod> icfg) {
		this.ideVizFile = ideVizFile;
		this.icfg = icfg;
	}

	
	@Override
	public void callRules(Query q, Set<Rule<Statement, INode<Val>, W>> allRules) {
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
	public void reachableNodes(Query q, Map<Transition<Statement, INode<Val>>, W> reachedStates) {
		for(Entry<Transition<Statement, INode<Val>>, W> e : reachedStates.entrySet()){
			Transition<Statement, INode<Val>> key = e.getKey();
			Statement stmt = key.getLabel();
			if(stmt.getMethod() == null)
				continue;
			Map<Transition<Statement, INode<Val>>, W> transInMethod = getOrCreate(q,stmt.getMethod());
			transInMethod.put(e.getKey(), e.getValue());
		}
	}

	private Map<Transition<Statement, INode<Val>>, W> getOrCreate(Query q, SootMethod method) {
		Map<Transition<Statement, INode<Val>>, W> map = reachedNodes.get(q, method);  
		if(map != null)
			return map;
		reachedNodes.put(q, method, Maps.<Transition<Statement, INode<Val>>, W>newHashMap());
		return reachedNodes.get(q, method);
	}

	@Override
	public void done(){
		JSONArray eventualData = new JSONArray();
		for(Query query : reachedNodes.rowKeySet()){
			JSONQuery queryJSON = new JSONQuery(query);
			JSONArray data = new JSONArray();
			for(SootMethod m : reachedNodes.columnKeySet()){
				Map<Transition<Statement, INode<Val>>, W> transitionsInMethod = getOrCreate(query,m);
				if(transitionsInMethod.isEmpty())
					continue;
				int labelYOffset = computeLabelYOffset(getFacts(transitionsInMethod));
				JSONMethod jsonMethod = new JSONMethod(m);
				IDEVizDebugger<W>.JSONControlFlowGraph cfg = createControlFlowGraph(m, labelYOffset);
				
				jsonMethod.put("cfg", cfg);

				Set<Rule<Statement, INode<Val>, W>> rulesInMethod = getOrCreateRuleSet(query,m);
				DataFlowGraph dfg = createDataFlowGraph(query, transitionsInMethod,rulesInMethod,cfg,m,labelYOffset);
				jsonMethod.put("dfg", dfg);
				data.add(jsonMethod);
			}
			queryJSON.put("methods",data);
			eventualData.add(queryJSON);
		};
		
		try (FileWriter file = new FileWriter(ideVizFile)) {
			file.write(eventualData.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	private int computeLabelYOffset(Set<Val> facts) {
		int labelYOffset = 0;
		for (Val g : facts) {
			labelYOffset = Math.max(labelYOffset, charSize * g.toString().length());
		}
		return labelYOffset;
	}

	private DataFlowGraph createDataFlowGraph(Query q, Map<Transition<Statement, INode<Val>>, W> transitionsInMethod,
			Set<Rule<Statement, INode<Val>, W>> rulesInMethod, JSONControlFlowGraph cfg, SootMethod m, int labelYOffset) {
		LinkedList<Val> factsList = new LinkedList<>();
		DataFlowGraph dataFlowGraph = new DataFlowGraph();
		// System.out.println("Number of facts:\t" + esg.getFacts().size());
		Set<Val> facts = getFacts(transitionsInMethod);
		JSONArray data = new JSONArray();

		int offset = 0;
		int charSize = 8;
		for (Val u : facts) {
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

		// System.out.println("Number of nodes:\t" + esg.getNodes().size());
		for (Entry<Transition<Statement, INode<Val>>, W> trans : transitionsInMethod.entrySet()) {
			Stmt stmt = trans.getKey().getLabel().getUnit().get();
			Val val = trans.getKey().getStart().fact();
			if(!trans.getKey().getLabel().getMethod().equals(val.m()))
				continue;
			JSONObject nodeObj = new JSONObject();
			JSONObject pos = new JSONObject();
			pos.put("x", (factsList.indexOf(val) + 1) * 30 /*+ offset * charSize*/);
			pos.put("y",
					(cfg.stmtsList.indexOf(trans.getKey().getLabel().getUnit().get())) * 30 + 30 /*+ labelYOffset*/);

			nodeObj.put("position", pos);
			String classes = "esgNode method" + id(m) + " ";

			JSONObject additionalData = new JSONObject();
			additionalData.put("id", "q"+id(q)+"n" + id(new Node<Statement,Val>(trans.getKey().getLabel(),val)));
			additionalData.put("stmtId", id(stmt));
			additionalData.put("factId", id(val));
			nodeObj.put("classes", classes);
			nodeObj.put("group", "nodes");
			nodeObj.put("data", additionalData);

			data.add(nodeObj);
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
			dataEntry.put("source", "q"+id(q)+ "n" + id(start));
			dataEntry.put("target",  "q"+id(q)+"n" + id(target));
			dataEntry.put("directed", "true");
			dataEntry.put("direction", (q instanceof BackwardQuery ? "Backward" : "Forward"));
			nodeObj.put("data", dataEntry);
			nodeObj.put("classes", "esgEdge  method" + id(m));
			nodeObj.put("group", "edges");
			data.add(nodeObj);
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


	private Set<Val> getFacts(Map<Transition<Statement, INode<Val>>, W> transitionsInMethod) {
		Set<Val> values = Sets.newHashSet();
		for(Transition<Statement, INode<Val>> t : transitionsInMethod.keySet())
			values.add(t.getStart().fact());
		return values;
	}
	
	private JSONControlFlowGraph createControlFlowGraph(SootMethod m, int labelYOffset){
		IDEVizDebugger<W>.JSONControlFlowGraph cfg = new JSONControlFlowGraph();
		int index = 0;	
		int offset = 0;
		JSONArray data = new JSONArray();
		for (Unit u : m.getActiveBody().getUnits()) {
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
				for (SootMethod callee : icfg.getCalleesOfCallAt(u)){
					if(callee != null && callee.toString() != null){
						callees.add(new JSONMethod(callee));
					}
				}
				label.put("callees", callees);
			}
			if (icfg.isExitStmt(u)) {
				label.put("returnSite", icfg.isExitStmt(u));
				JSONArray callees = new JSONArray();
				Set<SootMethod> callers = new HashSet<>();
				for (Unit callsite : icfg.getCallersOf(icfg.getMethodOf(u)))
					callers.add(icfg.getMethodOf(callsite));
	
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
	public String getShortLabel(Unit u) {
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
