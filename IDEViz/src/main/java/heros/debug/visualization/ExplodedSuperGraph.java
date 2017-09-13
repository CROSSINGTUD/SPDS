package heros.debug.visualization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import heros.InterproceduralCFG;
import heros.debug.visualization.IDEToJSON.Direction;
import heros.solver.Pair;

public class ExplodedSuperGraph<Method, Stmt, Fact, Value> {

	final Method method;
	final Direction direction;
	private LinkedList<Fact> facts = new LinkedList<>();
	private LinkedList<ESGNode> nodes = new LinkedList<>();
	private LinkedList<CalleeESGNode> calleeNodes = new LinkedList<>();
	private LinkedList<ESGEdge> edges = new LinkedList<>();
	private Multimap<Stmt,Object> informationAtStmt = HashMultimap.create();
	private Set<Pair<ESGNode, ESGNode>> summaries = new HashSet<>();
	private static int esgNodeCounter = 0;
	private EdgeLabels labels;
	private Map<ESGNode, Value> esgNodeToValue = new HashMap<>();

	public void setValue(ESGNode esgNode, Value value) {
		esgNodeToValue.put(esgNode,value);
	}
	public ExplodedSuperGraph(Method m, Direction dir) {
		this.method = m;
		this.direction = dir;
		this.labels = new DefaultLabels();
	}

	public interface EdgeLabels {
		public String summaryFlow();

		public String normalFlow();

		public String returnFlow();

		public String call2ReturnFlow();

		public String callFlow();
	}

	private class DefaultLabels implements EdgeLabels {

		@Override
		public String summaryFlow() {
			return "summaryFlow";
		}

		@Override
		public String normalFlow() {
			return "normalFlow";
		}

		@Override
		public String returnFlow() {
			return "returnFlow";
		}

		@Override
		public String call2ReturnFlow() {
			return "call2ReturnFlow";
		}

		@Override
		public String callFlow() {
			return "callFlow";
		}
	}

	public void normalFlow(Stmt start, Fact startFact, Stmt target, Fact targetFact) {
		addEdge(new ESGEdge(new ESGNode(start, startFact), new ESGNode(target, targetFact), labels.normalFlow()));
	}

	public void callFlow(Stmt start, Fact startFact, Stmt target, Fact targetFact) {
		ESGNode callSiteNode = new ESGNode(start, startFact);
		CalleeESGNode calleeNode = new CalleeESGNode(target, targetFact, callSiteNode);
		addEdge(new ESGEdge(callSiteNode, calleeNode, labels.callFlow()));
	}

	public void callToReturn(Stmt start, Fact startFact, Stmt target, Fact targetFact) {
		addEdge(new ESGEdge(new ESGNode(start, startFact), new ESGNode(target, targetFact), labels.call2ReturnFlow()));
	}

	public void returnFlow(Stmt start, Fact startFact, Stmt target, Fact targetFact) {
		ESGNode nodeInMethod = new ESGNode(target, targetFact);
		addEdge(new ESGEdge(new CalleeESGNode(start, startFact, nodeInMethod), nodeInMethod, labels.returnFlow()));
	}

	public void addEdgeWithLabel(Method m, ESGNode start, ESGNode target, String label) {
		addEdge(new ESGEdge(start, target, label));
	}

	public void addSummary(ESGNode start, ESGNode target) {
		summaries.add(new Pair<ESGNode, ESGNode>(start, target));
	}
	
	public void addInformationForStatement(Stmt stmt,Object information){
		informationAtStmt.put(stmt, information);
	}

	public Multimap<Stmt, Object> getInformationPerStmt() {
		return informationAtStmt;
	}
	void addNode(ESGNode g) {
		if (!nodes.contains(g))
			nodes.add(g);
		if (!facts.contains(g.a) && !(g instanceof ExplodedSuperGraph.CalleeESGNode))
			facts.add(g.a);
		if (g instanceof ExplodedSuperGraph.CalleeESGNode)
			calleeNodes.add((CalleeESGNode) g);
	}

	void addEdge(ESGEdge g) {
		addNode(g.start);
		addNode(g.target);
		if (!edges.contains(g))
			edges.add(g);
	}

	protected class CalleeESGNode extends ESGNode {

		ESGNode linkedNode;

		CalleeESGNode(Stmt u, Fact a, ESGNode linkedNode) {
			super(u, a);
			this.linkedNode = linkedNode;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((linkedNode == null) ? 0 : linkedNode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			CalleeESGNode other = (CalleeESGNode) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (linkedNode == null) {
				if (other.linkedNode != null)
					return false;
			} else if (!linkedNode.equals(other.linkedNode))
				return false;
			return true;
		}

		private ExplodedSuperGraph getOuterType() {
			return ExplodedSuperGraph.this;
		}
	}

	public class ESGNode {
		Stmt u;
		Fact a;

		public ESGNode(Stmt u, Fact a) {
			this.u = u;
			this.a = a;
			esgNodeCounter++;
			if (esgNodeCounter % 10000 == 0) {
				System.err.println("Warning: Using JSONOutputDebugger, might slow down performance.");
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((u == null) ? 0 : u.hashCode());
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
			ESGNode other = (ESGNode) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (u == null) {
				if (other.u != null)
					return false;
			} else if (!u.equals(other.u))
				return false;
			return true;
		}

		private ExplodedSuperGraph getOuterType() {
			return ExplodedSuperGraph.this;
		}
	}

	public class ESGEdge {
		public final ESGNode start;
		public final ESGNode target;
		public final String labels;

		public ESGEdge(ESGNode start, ESGNode target, String type) {
			this.start = start;
			this.target = target;
			this.labels = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((start == null) ? 0 : start.hashCode());
			result = prime * result + ((target == null) ? 0 : target.hashCode());
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
			ESGEdge other = (ESGEdge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (start == null) {
				if (other.start != null)
					return false;
			} else if (!start.equals(other.start))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}

		private ExplodedSuperGraph getOuterType() {
			return ExplodedSuperGraph.this;
		}

	}

	public List<Fact> getFacts() {
		return facts;
	}

	public List<ESGNode> getNodes() {
		return nodes;
	}

	public LinkedList<ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGEdge> getEdges() {
		return edges;
	}

	public void linkSummaries(InterproceduralCFG<Stmt, Method> icfg) {
		for (Pair<ESGNode, ESGNode> p : summaries) {
			ESGNode start = p.getO1();
			ESGNode target = p.getO2();
			Set<CalleeESGNode> starts = new HashSet<>();
			Set<CalleeESGNode> targets = new HashSet<>();
			for (CalleeESGNode n : calleeNodes) {
				if (n.a.equals(start.a) && (n.u.equals(start.u)))
					starts.add(n);
				if (n.a.equals(target.a) && (n.u.equals(target.u)))
					targets.add(n);
			}
			for (CalleeESGNode summaryStart : starts) {
				for (CalleeESGNode summaryTarget : targets) {
					if (icfg.getSuccsOf(summaryStart.linkedNode.u).contains(summaryTarget.linkedNode.u))
						addEdge(new ESGEdge(summaryStart, summaryTarget, labels.summaryFlow()));
					if (direction == Direction.Forward) {
						if (icfg.getSuccsOf(summaryStart.linkedNode.u).contains(summaryTarget.linkedNode.u))
							addEdge(new ESGEdge(summaryStart, summaryTarget,labels.summaryFlow()));
					} else if (direction == Direction.Backward) {
						if (icfg.getPredsOf(summaryStart.linkedNode.u).contains(summaryTarget.linkedNode.u))
							addEdge(new ESGEdge(summaryStart, summaryTarget, labels.summaryFlow()));
					}
				}
			}
		}
	}
	public Value getIDEValue(ExplodedSuperGraph<Method, Stmt, Fact, Value>.ESGNode node) {
		return esgNodeToValue.get(node);
	}
}
