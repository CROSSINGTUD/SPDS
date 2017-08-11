package analysis.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import analysis.INode;
import analysis.Node;
import analysis.NodeWithLocation;
import analysis.Solver;
import wpds.impl.Rule;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.wildcard.Wildcard;

public class AbstractTest {
	private Multimap<Node<Stmt, Variable>, Node<Stmt, Variable>> successorMap = HashMultimap.create();
	private Table<Node<Stmt, Variable>, Node<Stmt, Variable>, Rule<Statement, INode<Stmt, Variable>, NoWeight<Statement>>> callSiteRuleMap = HashBasedTable
			.create();
	private Table<NodeWithLocation<Stmt, Variable, FieldRef>, NodeWithLocation<Stmt, Variable, FieldRef>, Rule<FieldRef, NodeWithLocation<Stmt, Variable, FieldRef>, NoWeight<FieldRef>>> fieldRefRuleMap = HashBasedTable
			.create();

	private void addFieldPop(Node<Stmt, Variable> curr, FieldRef pop, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		fieldRefRuleMap.put(asField(curr), asField(succ), new UPopRule<FieldRef, NodeWithLocation<Stmt, Variable,FieldRef>>(asField(curr), pop, asField(succ)));
		callSiteRuleMap.put(curr, succ, new UNormalRule<Statement, INode<Stmt, Variable>>(curr, new CallWildCard(), succ, new CallWildCard()));
	}

	private void addFieldPush(Node<Stmt, Variable> curr, FieldRef push, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		fieldRefRuleMap.put(asField(curr), asField(succ), new UPushRule<FieldRef, NodeWithLocation<Stmt, Variable, FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard(),push));
		callSiteRuleMap.put(curr, succ, new UNormalRule<Statement, INode<Stmt, Variable>>(curr, new CallWildCard(), succ, new CallWildCard()));
	}


	private void addNormal(Node<Stmt, Variable> curr, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		fieldRefRuleMap.put(asField(curr), asField(succ), new UNormalRule<FieldRef, NodeWithLocation<Stmt, Variable,FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard()));
		callSiteRuleMap.put(curr, succ, new UNormalRule<Statement, INode<Stmt, Variable>>(curr, new CallWildCard(), succ, new CallWildCard()));
	}
	private void addFieldNormal(Node<Stmt, Variable> curr, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		fieldRefRuleMap.put(asField(curr), asField(succ), new UNormalRule<FieldRef, NodeWithLocation<Stmt, Variable, FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard()));
	}

	private void addCallSitePop(Node<Stmt, Variable> curr, Statement pop, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		callSiteRuleMap.put(curr, succ, new UPopRule<Statement, INode<Stmt, Variable>>(curr, pop, succ));
		addFieldNormal(curr, succ);
	}

	private void addCallSitePush(Node<Stmt, Variable> curr, Statement callSite, Node<Stmt, Variable> succ, Statement returnSite, Statement calleeStart) {
		addSucc(curr, succ);
		callSiteRuleMap.put(curr, succ, new UPushRule<Statement, INode<Stmt, Variable>>(curr, callSite,succ,calleeStart,returnSite));
		addFieldNormal(curr, succ);
	} 
	private void addCallSiteNormal(Node<Stmt, Variable> curr, Node<Stmt, Variable> succ) {
		addSucc(curr, succ);
		callSiteRuleMap.put(curr, succ, new UNormalRule<Statement, INode<Stmt, Variable>>(curr, new CallWildCard(), succ, new CallWildCard()));
	}
	private void addSucc(Node<Stmt, Variable> curr, Node<Stmt, Variable> succ) {
		successorMap.put(curr, succ);
	}
	private FieldRef epsilonField = new FieldRef("eps_f");
	private Statement epsilonCallSite = new Statement("eps_c");
	
	private NodeWithLocation<Stmt, Variable, Statement> asStatement(Node<Stmt,Variable> node){
		return asStatement(node, solver.emptyCallSite());
	}
	

	private NodeWithLocation<Stmt, Variable, Statement> asStatement(Node<Stmt, Variable> node, Statement statement) {
		return new NodeWithLocation<Stmt, Variable, Statement>(node.stmt(), node.fact(), statement);
	}
	
	private NodeWithLocation<Stmt, Variable, FieldRef> asField(Node<Stmt,Variable> node){
		return new NodeWithLocation<Stmt, Variable, FieldRef>(node.stmt(), node.fact(), solver.emptyField());
	}
	private Solver<Stmt, Variable, FieldRef, Statement> solver = new Solver<Stmt, Variable, FieldRef, Statement>() {
		@Override
		public Rule<Statement, INode<Stmt, Variable>, NoWeight<Statement>> computeCallSiteRule(Node<Stmt, Variable> curr,
				Node<Stmt, Variable> succ) {
			return callSiteRuleMap.get(curr, succ);
		}

		@Override
		public Rule<FieldRef, NodeWithLocation<Stmt, Variable, FieldRef>, NoWeight<FieldRef>> computeFieldRefRule(NodeWithLocation<Stmt, Variable, FieldRef> curr,
				NodeWithLocation<Stmt, Variable, FieldRef> succ) {
			return fieldRefRuleMap.get(curr, succ);
		}

		@Override
		public Collection<Node<Stmt, Variable>> computeSuccessor(Node<Stmt, Variable> node) {
			return successorMap.get(node);
		}

		@Override
		public FieldRef epsilonField() {
			return epsilonField;
		}

		@Override
		public Statement epsilonCallSite() {
			return epsilonCallSite;
		}

		@Override
		public FieldRef emptyField() {
			return new FieldRef("EMPTY_F");
		}

		@Override
		public Statement emptyCallSite() {
			return new Statement("EMPTY_C");
		}
	};

	@Test
	public void test1() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addCallSitePush(s("2","v"),solver.emptyCallSite(), s("3","p"),call("cs1"),call("foo"));
		addFieldPush(s("3","p"), f("g"), s("4","q"));
		addCallSitePop(s("4","q"), call("foo"), s("5","w"));
		addFieldPop(s("5","w"), f("g"), s("6","x"));
		addFieldPop(s("6","x"), f("f"), s("7","y"));
		
//		second branch
		addFieldPush(s("8","r"), f("f"), s("9","s"));
		addCallSitePush(s("9","s"),call("cs1"), s("3","p"),call("cs2"),call("foo"));
		addCallSitePop(s("4","q"), call("foo"), s("10","t"));
		solver.solve(s("1","u"));
//		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(s("10","t")));
		assertTrue(solver.getReachedStates().contains(s("5","w")));
		assertTrue(solver.getReachedStates().contains(s("6","x")));
		assertFalse(solver.getReachedStates().contains(s("7","y")));
	}
	
	@Test
	public void testWithTwoStacks() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addCallSitePush(s("2","v"), solver.emptyCallSite(), s("3","p"), solver.emptyCallSite(),call("foo"));
		addFieldPush(s("3","p"), f("g"), s("4","q"));
		addCallSitePop(s("4","q"), call("foo"), s("5","w"));
		solver.solve(s("1","u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(s("5","w")));
	}


	@Test
	public void positiveTestFieldDoublePushAndPop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPush(s("2","v"), f("g"), s("3","w"));
		addFieldPop(s("3","w"), f("g"), s("4","x"));
		addFieldPop(s("4","x"), f("h"), s("5","y"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("4","x")));
		assertTrue(solver.getReachedStates().contains(s("5","y")));
	}

	@Test
	public void negativeTestFieldDoublePushAndPop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPush(s("2","v"), f("h"), s("3","w"));
		addFieldPop(s("3","w"), f("h"), s("4","x"));
		addFieldPop(s("4","x"), f("g"), s("5","y"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("4","x")));
		assertFalse(solver.getReachedStates().contains(s("5","y")));
	}
	
	@Test
	public void positiveTestFieldPushAndPop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPop(s("2","v"), f("h"), s("3","w"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("3","w")));
	}
	
	@Test
	public void positiveTestFieldIntermediatePushAndPop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addNormal(s("2","v"), s("3","w"));
		addNormal(s("3","w"), s("4","w"));
		addFieldPop(s("4","w"), f("h"), s("5","w"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("5","w")));
	}
	
	@Test
	public void positiveTestFieldLoop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPush(s("2","v"), f("h"), s("2","v"));
		addFieldPop(s("2","v"), f("h"), s("3","w"));
		addFieldPop(s("3","w"), f("h"), s("4","x"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("4","x")));
	}
	
	@Test
	public void positiveTestFieldLoop2() {
		addFieldPush(s("0","a"), f("g"), s("1","u"));
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPush(s("2","v"), f("h"), s("2","v"));
		addFieldPop(s("2","v"), f("h"), s("3","w"));
		addFieldPop(s("3","w"), f("h"), s("4","x"));
		addFieldPop(s("4","x"), f("g"), s("5","y"));
		solver.solve(s("0","a"));
		assertTrue(solver.getReachedStates().contains(s("5","y")));
	}
	
//	@Test
//	public void positiveSummaryTest() {
////		a.g = c
////		foo(a)
////		e = a.f
////		foo(e)
////		h = e.f
//		addFieldPush(s("0","c"), f("g"), s("1","a"));
//		addCallSitePush(s("1","a"), call("foo1"), s("1a","u"));
//		addFieldPush(s("1a","u"), f("f"), s("2a","u"));
//		addCallSitePop(s("2a","u"), call("foo1"), s("2","a"));
//		addFieldPop(s("2","a"), f("f"), s("3","e"));
//		addCallSitePush(s("3","e"), call("foo2"), s("1a","u"));
//		addCallSitePop(s("2a","u"), call("foo2"), s("4","e"));
//		addFieldPop(s("4","e"), f("f"), s("5","h"));
//		solver.solve(s("0","c"));
//		assertTrue(solver.getReachedStates().contains(s("4","e")));
//	}
	
	@Test
	public void negativeTestFieldPushAndPop() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addFieldPop(s("2","v"), f("f"), s("3","w"));
		solver.solve(s("1","u"));
		assertFalse(solver.getReachedStates().contains(s("3","w")));
	}
	@Test
	public void negativeTestCallSitePushAndPop() {
		addCallSitePush(s("1","u"), solver.emptyCallSite(),s("2","v"),solver.epsilonCallSite(),call("h"));
		addCallSitePop(s("2","v"), call("f"), s("3","w"));
		solver.solve(s("1","u"));
		assertFalse(solver.getReachedStates().contains(s("3","w")));
	}
	
	@Test
	public void positiveTestCallSitePushAndPop() {
		addCallSitePush(s("1","u"),  solver.emptyCallSite(),s("2","v"),solver.emptyCallSite(),call("h"));
		addCallSitePop(s("2","v"), call("h"), s("3","w"));
		solver.solve(s("1","u"));
		assertTrue(solver.getReachedStates().contains(s("3","w")));
	}
	
	private static Statement call(String call) {
		return new Statement(call);
	}

	private static FieldRef f(String f) {
		return new FieldRef(f);
	}

	public static Node<Stmt,Variable> s(String stmt,String var){
		return new Node<Stmt,Variable>(new Stmt(stmt),new Variable(var));
	}
	
	private static class Stmt extends StringBasedObj {
		public Stmt(String name) {
			super(name);
		}
	}

	private static class Variable extends StringBasedObj {
		public Variable(String name) {
			super(name);
		}
	}
	
	private static class CallWildCard extends Statement implements Wildcard {
		public CallWildCard() {
			super("*_c");
		}
	}

	private static class FieldWildCard extends FieldRef implements Wildcard {
		public FieldWildCard() {
			super("*_f");
		}
	}

	private static class FieldRef extends StringBasedObj implements Location {
		public FieldRef(String name) {
			super(name);
		}
	}

	private static class Statement extends StringBasedObj implements Location {
		public Statement(String name) {
			super(name);
		}
	}

	private static class StringBasedObj {
		final private String name;

		public StringBasedObj(String name) {
			this.name = name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			StringBasedObj other = (StringBasedObj) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return name;
		}
	}
}
