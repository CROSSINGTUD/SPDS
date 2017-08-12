package analysis.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import analysis.INode;
import analysis.Node;
import analysis.NodeWithLocation;
import analysis.PopNode;
import analysis.PushNode;
import analysis.SingleNode;
import analysis.Solver;
import analysis.Solver.PDSSystem;
import wpds.impl.Rule;
import wpds.impl.UNormalRule;
import wpds.impl.UPopRule;
import wpds.impl.UPushRule;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.Location;
import wpds.wildcard.Wildcard;

public class AbstractTest {
	private Multimap<Node<Statement, Variable>, Node<Statement, Variable>> successorMap = HashMultimap.create();
	private Table<Node<Statement, Variable>, Node<Statement, Variable>, Collection<Rule<Statement, INode<Variable>, NoWeight<Statement>>>> callSiteRuleMap = HashBasedTable
			.create();
	private Table<NodeWithLocation<Statement, Variable, FieldRef>, NodeWithLocation<Statement, Variable, FieldRef>, Collection<Rule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>, NoWeight<FieldRef>>>> fieldRefRuleMap = HashBasedTable
			.create();

	private void addFieldPop(Node<Statement, Variable> curr, FieldRef pop, Node<Statement, Variable> succ) {
		addSucc(curr, new PopNode<Statement, Variable, FieldRef>(succ.stmt(),succ.fact(),pop,PDSSystem.FIELDS));
	}

	private void addFieldPush(Node<Statement, Variable> curr, FieldRef push, Node<Statement, Variable> succ) {
		addSucc(curr, new PushNode<Statement, Variable, FieldRef>(succ.stmt(),succ.fact(),push,PDSSystem.FIELDS));
	}

	private void addNormal(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
	}

	private void addReturnFlow(Node<Statement, Variable> curr, Statement pop, Node<Statement, Variable> succ) {
		addSucc(curr, new PopNode<Statement, Variable, Statement>(succ.stmt(),succ.fact(),pop, PDSSystem.METHODS));
	}
	
	private void addCallFlow(Node<Statement, Variable> curr, Node<Statement, Variable> succ, Statement push) {
		addSucc(curr, new PushNode<Statement, Variable, Statement>(succ.stmt(),succ.fact(),push, PDSSystem.METHODS));
	} 
	private void addSucc(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		successorMap.put(curr, succ);
	}
	private FieldRef epsilonField = new FieldRef("eps_f");
	private Statement epsilonCallSite = new Statement("eps_c");
	
	private Solver<Statement, Variable, FieldRef> solver = new Solver<Statement, Variable, FieldRef>() {

		@Override
		public Collection<Node<Statement, Variable>> computeSuccessor(Node<Statement, Variable> node) {
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

		@Override
		public FieldRef fieldWildCard() {
			return new FieldWildCard();
		}
	};

	@Test
	public void test1() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addCallFlow(node("2","v"), node("3","p"),call("foo"));
		addFieldPush(node("3","p"), f("g"), node("4","q"));
		addReturnFlow(node("4","q"), call("foo"), node("5","w"));
		addFieldPop(node("5","w"), f("g"), node("6","x"));
		addFieldPop(node("6","x"), f("f"), node("7","y"));
		
//		second branch
		addFieldPush(node("8","r"), f("f"), node("9","s"));
		addCallFlow(node("9","s"),node("3","p"),call("foo"));
		addReturnFlow(node("4","q"), call("foo"), node("10","t"));
		addFieldPush(node("10","t"), f("f"), node("11","s"));
		
		solver.solve(node("1","u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(node("10","t")));
		assertTrue(solver.getReachedStates().contains(node("5","w")));
		assertTrue(solver.getReachedStates().contains(node("6","x")));
		assertFalse(solver.getReachedStates().contains(node("7","y")));
	}
	
	@Test
	public void simple() {
		addNormal(node("1","v"), node("2","w"));
		addCallFlow(node("2","w"), node("3","p"),call("4"));
		addNormal(node("4","p"),  node("5","q"));
		addNormal(node("5","q"),  node("6","x"));
		addReturnFlow(node("6","x"), call("4"),node("6","p"));
		
		solver.solve(node("1","v"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node("6","p")));
	}
	
	
	@Test
	public void testWithTwoStacks() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addCallFlow(node("2","v"),  node("3","p"), call("4"));
		addFieldPush(node("4","p"), f("g"), node("5","q"));
		addReturnFlow(node("5","q"), call("5"), node("3","w"));
		addNormal(node("3","w"),node("7","t"));
		solver.solve(node("1","u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node("7","t")));
	}


	@Test
	public void positiveTestFieldDoublePushAndPop() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPush(node("2","v"), f("g"), node("3","w"));
		addFieldPop(node("3","w"), f("g"), node("4","x"));
		addFieldPop(node("4","x"), f("h"), node("5","y"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("4","x")));
		assertTrue(solver.getReachedStates().contains(node("5","y")));
	}

	@Test
	public void negativeTestFieldDoublePushAndPop() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPush(node("2","v"), f("h"), node("3","w"));
		addFieldPop(node("3","w"), f("h"), node("4","x"));
		addFieldPop(node("4","x"), f("g"), node("5","y"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("4","x")));
		assertFalse(solver.getReachedStates().contains(node("5","y")));
	}
	
	@Test
	public void positiveTestFieldPushAndPop() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPop(node("2","v"), f("h"), node("3","w"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("3","w")));
	}
	
	@Test
	public void positiveTestFieldIntermediatePushAndPop() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addNormal(node("2","v"), node("3","w"));
		addNormal(node("3","w"), node("4","w"));
		addFieldPop(node("4","w"), f("h"), node("5","w"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("5","w")));
	}
	
	@Test
	public void positiveTestFieldLoop() {
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPush(node("2","v"), f("h"), node("2","v"));
		addFieldPop(node("2","v"), f("h"), node("3","w"));
		addFieldPop(node("3","w"), f("h"), node("4","x"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("4","x")));
	}
	
	@Test
	public void positiveTestFieldLoop2() {
		addFieldPush(node("0","a"), f("g"), node("1","u"));
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPush(node("2","v"), f("h"), node("2","v"));
		addFieldPop(node("2","v"), f("h"), node("3","w"));
		addFieldPop(node("3","w"), f("h"), node("4","x"));
		addFieldPop(node("4","x"), f("g"), node("5","y"));
		solver.solve(node("0","a"));
		assertTrue(solver.getReachedStates().contains(node("5","y")));
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
		addFieldPush(node("1","u"), f("h"), node("2","v"));
		addFieldPop(node("2","v"), f("f"), node("3","w"));
		solver.solve(node("1","u"));
		assertFalse(solver.getReachedStates().contains(node("3","w")));
	}
	@Test
	public void negativeTestCallSitePushAndPop() {
		addCallFlow(node("1","u"), node("2","v"),call("3"));
		addReturnFlow(node("3","v"), call("3"), node("3","w"));
		addNormal(node("3","w"), node("4","w"));
		solver.solve(node("1","u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(node("3","w")));
	}
	
	@Test
	public void positiveTestCallSitePushAndPop() {
		addCallFlow(node("1","u"), node("2","v"),call("h"));
		addReturnFlow(node("2","v"), call("h"), node("3","w"));
		solver.solve(node("1","u"));
		assertTrue(solver.getReachedStates().contains(node("3","w")));
	}
	
	private static Statement call(String call) {
		return new Statement(call);
	}

	private static FieldRef f(String f) {
		return new FieldRef(f);
	}

	public static Node<Statement,Variable> node(String stmt,String var){
		return new Node<Statement,Variable>(new Statement(stmt),new Variable(var));
	}
	
	private static class Statement extends StringBasedObj implements Location {
		public Statement(String name) {
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
