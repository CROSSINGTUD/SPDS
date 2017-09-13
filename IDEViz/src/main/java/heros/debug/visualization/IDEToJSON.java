package heros.debug.visualization;

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

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;

import heros.InterproceduralCFG;

public class IDEToJSON<Method, Stmt, Fact, Value, I extends InterproceduralCFG<Stmt, Method>> {

	private File jsonFile;
	private Map<Key, ExplodedSuperGraph<Method, Stmt, Fact, Value>> methodToCfg = new HashMap<>();
	private Map<Object, Integer> objectToInteger = new HashMap<>();
	private I icfg;

	public enum Direction {
		Forward, Backward
	}

	public IDEToJSON(File file, I icfg) {
		this.jsonFile = file;
		this.icfg = icfg;
	}

	public ExplodedSuperGraph<Method, Stmt, Fact, Value> getOrCreateESG(Method method, Direction dir) {
		ExplodedSuperGraph<Method, Stmt, Fact, Value> cfg = methodToCfg.get(new Key(method, dir));
		if (cfg == null) {
			cfg = new ExplodedSuperGraph<Method, Stmt, Fact, Value>(method, dir);
			methodToCfg.put(new Key(method, dir), cfg);
		}
		return cfg;
	}

	private class Key {
		final Method m;
		final Direction dir;

		private Key(Method m, Direction dir) {
			this.m = m;
			this.dir = dir;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dir == null) ? 0 : dir.hashCode());
			result = prime * result + ((m == null) ? 0 : m.hashCode());
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
			Key other = (Key) obj;
			if (dir != other.dir)
				return false;
			if (m == null) {
				if (other.m != null)
					return false;
			} else if (!m.equals(other.m))
				return false;
			return true;
		}
	}

	// public void setValue(Stmt start, Fact startFact, Value value) {
	// esgNodeToLatticeVal.put(new ESGNode(start, startFact), value);
	// }

	public Integer id(Object u) {
		if (objectToInteger.get(u) != null)
			return objectToInteger.get(u);
		int size = objectToInteger.size() + 1;
		objectToInteger.put(u, size);
		return size;
	}

	private JSONObject toJSONObject(ExplodedSuperGraph<Method, Stmt, Fact, Value> esg) {
		esg.linkSummaries(icfg);
		JSONObject o = new JSONObject();
		o.put("methodName", StringEscapeUtils.escapeHtml4(esg.method.toString()));
		o.put("methodId", id(esg.method));
		o.put("direction", esg.direction.toString());
		JSONArray data = new JSONArray();
		LinkedList<Stmt> stmtsList = new LinkedList<>();
		int offset = 0;
		int labelYOffset = 0;
		int charSize = 8;
		for (Fact g : esg.getFacts()) {
			labelYOffset = Math.max(labelYOffset, charSize * g.toString().length());
		}
		int index = 0;
		for (Stmt u : getListOfStmts(esg.method)) {
			JSONObject nodeObj = new JSONObject();
			JSONObject pos = new JSONObject();
			stmtsList.add(u);
			pos.put("x", 10);
			pos.put("y", stmtsList.size() * 30 + labelYOffset);
			nodeObj.put("position", pos);
			JSONObject label = new JSONObject();
			label.put("label", u.toString());
			label.put("shortLabel", getShortLabel(u));
			if (icfg.isCallStmt(u)) {
				label.put("callSite", icfg.isCallStmt(u));
				JSONArray callees = new JSONArray();
				for (Method callee : icfg.getCalleesOfCallAt(u)){
					if(callee != null && callee.toString() != null && esg != null){
						callees.add(new JSONMethod(callee, esg.direction));
					}
				}
				label.put("callees", callees);
			}
			if (icfg.isExitStmt(u)) {
				label.put("returnSite", icfg.isExitStmt(u));
				JSONArray callees = new JSONArray();
				Set<Method> callers = new HashSet<>();
				for (Stmt callsite : icfg.getCallersOf(icfg.getMethodOf(u)))
					callers.add(icfg.getMethodOf(callsite));

				for (Method caller : callers)
					callees.add(new JSONMethod(caller, esg.direction));
				label.put("callers", callees);
			}
			label.put("stmtId", id(u));
			label.put("id", "stmt" + id(u));

			label.put("stmtIndex", index);
			index++;

			nodeObj.put("data", label);
			nodeObj.put("classes", "stmt label " + (icfg.isExitStmt(u) ? " returnSite " : " ")
					+ (icfg.isCallStmt(u) ? " callSite " : " ") + " method" + id(esg.method) + " " + esg.direction);
			data.add(nodeObj);
			offset = Math.max(offset, getShortLabel(u).toString().length());

			for (Stmt succ : icfg.getSuccsOf(u)) {
				JSONObject cfgEdgeObj = new JSONObject();
				JSONObject dataEntry = new JSONObject();
				dataEntry.put("source", "stmt" + id(u));
				dataEntry.put("target", "stmt" + id(succ));
				dataEntry.put("directed", "true");
				cfgEdgeObj.put("data", dataEntry);
				cfgEdgeObj.put("classes", "cfgEdge label method" + id(esg.method) + " " + esg.direction);
				data.add(cfgEdgeObj);
			}
		}

		LinkedList<Fact> factsList = new LinkedList<>();
		// System.out.println("Number of facts:\t" + esg.getFacts().size());
		for (Fact u : esg.getFacts()) {
			JSONObject nodeObj = new JSONObject();
			JSONObject pos = new JSONObject();
			factsList.add(u);
			pos.put("x", factsList.size() * 30 + offset * charSize);
			pos.put("y", labelYOffset);
			nodeObj.put("position", pos);
			JSONObject label = new JSONObject();
			label.put("label", u.toString());
			label.put("factId", id(u));
			nodeObj.put("classes", "fact label method" + id(esg.method) + " " + esg.direction);
			nodeObj.put("data", label);
			data.add(nodeObj);
		}

		// System.out.println("Number of nodes:\t" + esg.getNodes().size());
		for (ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGNode node : esg.getNodes()) {
			JSONObject nodeObj = new JSONObject();
			JSONObject pos = new JSONObject();
			if (node instanceof ExplodedSuperGraph.CalleeESGNode) {
				ExplodedSuperGraph.CalleeESGNode calleeESGNode = (ExplodedSuperGraph.CalleeESGNode) node;
				pos.put("x", (factsList.indexOf(calleeESGNode.linkedNode.a) + 1) * 30 + 10 + offset * charSize);
				pos.put("y",
						(stmtsList.indexOf(calleeESGNode.linkedNode.u) + (esg.direction == Direction.Forward ? 0 : 1))
								* 30 + labelYOffset);
			} else {
				assert stmtsList.indexOf(node.u) != -1;
				pos.put("x", (factsList.indexOf(node.a) + 1) * 30 + offset * charSize);
				pos.put("y",
						(stmtsList.indexOf(node.u) + (esg.direction == Direction.Forward ? 0 : 1)) * 30 + labelYOffset);
			}

			nodeObj.put("position", pos);
			String classes = "esgNode method" + id(esg.method) + " " + esg.direction;

			JSONObject additionalData = new JSONObject();
			additionalData.put("id", "n" + id(node));
			additionalData.put("stmtId", id(node.u));
			additionalData.put("factId", id(node.a));
			if (esg.getIDEValue(node) != null)
				additionalData.put("ideValue", StringEscapeUtils.escapeHtml4(esg.getIDEValue(node).toString()));
			nodeObj.put("classes", classes);
			nodeObj.put("group", "nodes");
			nodeObj.put("data", additionalData);

			data.add(nodeObj);
		}
		Multimap<Stmt, Object> stmtToInfo = esg.getInformationPerStmt();
		for (Stmt stmt : stmtToInfo.keySet()) {
			int numberOfInfos = 1;
			for (Object info : stmtToInfo.get(stmt)) {
				JSONObject nodeObj = new JSONObject();
				JSONObject pos = new JSONObject();
				pos.put("x", -numberOfInfos * 30);
				pos.put("y", (stmtsList.indexOf(stmt) * 30 + labelYOffset));
				nodeObj.put("position", pos);
				String classes = "esgNode additional information method" + id(esg.method) + " " + esg.direction;

				JSONObject additionalData = new JSONObject();
				additionalData.put("stmtId", id(stmt));
				additionalData.put("ideValue", StringEscapeUtils.escapeHtml4(info.toString()));
				nodeObj.put("classes", classes);
				nodeObj.put("group", "nodes");
				nodeObj.put("data", additionalData);
				data.add(nodeObj);
			}
		}
		// System.out.println("Number of edges:\t" + esg.getEdges().size());
		for (ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGEdge edge : esg.getEdges()) {
			JSONObject nodeObj = new JSONObject();
			JSONObject dataEntry = new JSONObject();
			dataEntry.put("id", "e" + id(edge));
			dataEntry.put("source", "n" + id(edge.start));
			dataEntry.put("target", "n" + id(edge.target));
			dataEntry.put("directed", "true");
			dataEntry.put("direction", esg.direction.toString());
			nodeObj.put("data", dataEntry);
			nodeObj.put("classes", "esgEdge method" + id(esg.method) + " " + edge.labels + " " + esg.direction);
			nodeObj.put("group", "edges");
			data.add(nodeObj);
		}
		o.put("data", data);
		return o;
	}

	public String getShortLabel(Stmt u) {
		return u.toString();
	}

	public List<Stmt> getListOfStmts(Method method) {
		LinkedList<Stmt> worklist = new LinkedList<>();
		worklist.addAll(icfg.getStartPointsOf(method));
		Set<Stmt> visited = new HashSet<>();
		LinkedList<Stmt> result = new LinkedList<>();

		while (!worklist.isEmpty()) {
			Stmt curr = worklist.pollFirst();
			if (visited.contains(curr))
				continue;
			visited.add(curr);
			result.add(curr);
			for (Stmt succ : icfg.getSuccsOf(curr)) {
				worklist.add(succ);
			}
		}
		return result;
	}

	public void writeToFile() {
		try (FileWriter file = new FileWriter(jsonFile)) {
			JSONArray methods = new JSONArray();
			Set<Method> visitedMethods = new HashSet<>();
			Set<Direction> direction = new HashSet<>();
			for (ExplodedSuperGraph<Method, Stmt, Fact, Value> c : methodToCfg.values()) {
				if(c.getEdges().isEmpty())
					continue;
				if (visitedMethods.add(c.method)) {
					JSONObject method = new JSONObject();
					method.put("name", StringEscapeUtils.escapeHtml4(c.method.toString()));
					method.put("id", id(c.method));
					methods.add(method);
				}
				direction.add(c.direction);
			}
			for (Direction d : direction) {
				for (Method m : visitedMethods) {
					getOrCreateESG(m, d);
				}
			}
			JSONArray explodedSupergraphs = new JSONArray();
			for (ExplodedSuperGraph<Method, Stmt, Fact, Value> c : methodToCfg.values()) {
				explodedSupergraphs.add(toJSONObject(c));
			}
			JSONArray directionArray = new JSONArray();
			for (Direction d : direction) {
				JSONObject v = new JSONObject();
				v.put("value", d.toString());
				directionArray.add(v);
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("methodList", methods);
			jsonObject.put("explodedSupergraphs", explodedSupergraphs);
			jsonObject.put("directions", directionArray);

			file.write(jsonObject.toJSONString());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class JSONMethod extends JSONObject {

		JSONMethod(Method m, Direction dir) {
			this.put("name", StringEscapeUtils.escapeHtml4(m.toString()));
			this.put("id", id(m));
			this.put("direction", dir.toString());
		}
	}

}
