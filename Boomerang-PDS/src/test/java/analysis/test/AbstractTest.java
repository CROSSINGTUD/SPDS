package analysis.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import analysis.Node;
import analysis.NodeWithLocation;
import analysis.PopNode;
import analysis.PushNode;
import analysis.Solver;
import analysis.Solver.PDSSystem;
import wpds.interfaces.Location;
import wpds.interfaces.State;
import wpds.wildcard.Wildcard;

public class AbstractTest {
	private Multimap<Node<Statement, Variable>, State> successorMap = HashMultimap.create();

	private void addFieldPop(Node<Statement, Variable> curr,FieldRef ref, Node<Statement, Variable> succ) {
		addSucc(curr, new PopNode<NodeWithLocation<Statement,Variable,FieldRef>>(new NodeWithLocation<Statement,Variable,FieldRef>(succ.stmt(),succ.fact(), ref), PDSSystem.FIELDS));
	}
	private void addFieldPush(Node<Statement, Variable> curr, FieldRef push, Node<Statement, Variable> succ) {
		addSucc(curr, new PushNode<Statement, Variable, FieldRef>(succ.stmt(),succ.fact(),push,PDSSystem.FIELDS));
	}

	private void addNormal(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
	}

	private void addReturnFlow(Node<Statement, Variable> curr, Variable returns) {
		addSucc(curr, new PopNode<Variable>(returns, PDSSystem.METHODS));
	}
	
	private void addCallFlow(Node<Statement, Variable> curr, Node<Statement, Variable> succ, Statement returnSite) {
		addSucc(curr, new PushNode<Statement, Variable, Statement>(succ.stmt(),succ.fact(),returnSite, PDSSystem.METHODS));
	} 
	private void addSucc(Node<Statement, Variable> curr, State succ) {
		successorMap.put(curr, succ);
	}
	private FieldRef epsilonField = new FieldRef("eps_f");
	private Statement epsilonCallSite = new Statement(-1);
	
	private Solver<Statement, Variable, FieldRef> solver = new Solver<Statement, Variable, FieldRef>() {

		@Override
		public Collection<State> computeSuccessor(Node<Statement, Variable> node) {
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
			return new Statement(0);
		}

		@Override
		public FieldRef fieldWildCard() {
			return new FieldWildCard();
		}
	};
	@Test
	public void test1() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addCallFlow(node(2,"v"), node(3,"p"),returnSite(5));
		addFieldPush(node(3,"p"), f("g"), node(4,"q"));
		addReturnFlow(node(4,"q"), var("w"));
		addFieldPop(node(5,"w"), f("g"), node(6,"x"));
		addFieldPop(node(6,"x"), f("f"), node(7,"y"));
		
//		second branch
		addFieldPush(node(8,"r"), f("f"), node(9,"s"));
		addCallFlow(node(9,"s"),node(3,"p"),returnSite(10));
		addReturnFlow(node(4,"q"), var("t"));
		addFieldPush(node(10,"t"), f("f"), node(11,"s"));
		
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(node(10,"t")));
		assertTrue(solver.getReachedStates().contains(node(5,"w")));
		assertTrue(solver.getReachedStates().contains(node(6,"x")));
		assertFalse(solver.getReachedStates().contains(node(7,"y")));
	}
	
	@Test
	public void test1Simple() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addCallFlow(node(2,"v"), node(3,"p"),returnSite(5));
		addFieldPush(node(3,"p"), f("g"), node(4,"q"));
		addReturnFlow(node(4,"q"), var("w"));
		addFieldPop(node(5,"w"), f("g"), node(6,"x"));
		addFieldPop(node(6,"x"), f("f"), node(7,"y"));
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(6,"x")));
	}
	
	@Test
	public void simpleNonFieldFlow() {
		addNormal(node(1,"v"), node(2,"w"));
		addCallFlow(node(2,"w"), node(3,"p"),returnSite(4));
		addNormal(node(3,"p"),  node(5,"q"));
		addNormal(node(5,"q"),  node(6,"x"));
		addReturnFlow(node(6,"x"), var("p"));
		addNormal(node(4,"p"),  node(6,"y"));
		
		solver.solve(node(1,"v"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(6,"y")));
	}
	
	
	@Test
	public void testWithTwoStacks() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addCallFlow(node(2,"v"),  node(3,"p"), returnSite(4));
		addFieldPush(node(3,"p"), f("g"), node(5,"q"));
		addReturnFlow(node(5,"q"), var("w"));
		addNormal(node(4,"w"),node(7,"t"));
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(7,"t")));
	}


	@Test
	public void positiveTestFieldDoublePushAndPop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPush(node(2,"v"), f("g"), node(3,"w"));
		addFieldPop(node(3,"w"),f("g"), node(4,"x"));
		addFieldPop(node(4,"x"),f("h"), node(5,"y"));
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(4,"x")));
		assertTrue(solver.getReachedStates().contains(node(5,"y")));
	}

	@Test
	public void negativeTestFieldDoublePushAndPop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPush(node(2,"v"), f("h"), node(3,"w"));
		addFieldPop(node(3,"w"), f("h"), node(4,"x"));
		addFieldPop(node(4,"x"), f("g"), node(5,"y"));
		solver.solve(node(1,"u"));
		assertTrue(solver.getReachedStates().contains(node(4,"x")));
		assertFalse(solver.getReachedStates().contains(node(5,"y")));
	}

	@Test
	public void positiveTestFieldPushPushAndPop() {
		addFieldPush(node(0,"u"), f("h"), node(1,"u"));
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("h"),  node(2,"x"));
		solver.solve(node(0,"u"));
		assertTrue(solver.getReachedStates().contains(node(2,"x")));
	}
	@Test
	public void negativeTestFieldPushAndPopPop() {
		addFieldPush(node(0,"u"), f("h"), node(1,"u"));
		addFieldPop(node(1,"u"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("h"),  node(2,"x"));
		solver.solve(node(0,"u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(node(2,"x")));
	}
	@Test
	public void positiveTestFieldPushAndPop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("h"),  node(2,"x"));
		solver.solve(node(1,"u"));
		assertTrue(solver.getReachedStates().contains(node(2,"x")));
	}
	
	@Test
	public void positiveTestFieldIntermediatePushAndPop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addNormal(node(2,"v"), node(3,"w"));
		addNormal(node(3,"w"), node(4,"w"));
		addFieldPop(node(4,"w"), f("h"), node(5,"w"));
		solver.solve(node(1,"u"));
		assertTrue(solver.getReachedStates().contains(node(5,"w")));
	}
	
	@Test
	public void positiveTestFieldLoop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPush(node(2,"v"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("h"), node(3,"w"));
		addFieldPop(node(3,"w"), f("h"), node(4,"x"));
		solver.solve(node(1,"u"));
		assertTrue(solver.getReachedStates().contains(node(4,"x")));
	}
	
	@Test
	public void positiveTestFieldLoop2() {
		addFieldPush(node(0,"a"), f("g"), node(1,"u"));
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPush(node(2,"v"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("h"), node(3,"w"));
		addFieldPop(node(3,"w"), f("h"), node(4,"x"));
		addFieldPop(node(4,"x"), f("g"), node(5,"y"));
		solver.solve(node(0,"a"));
		assertTrue(solver.getReachedStates().contains(node(5,"y")));
	}
	
	@Test
	public void positiveSummaryTest() {
//		1 :a.g = c
//		4: foo(a)
//		5: e = a.f
//		6: foo(e)
//		7: h = e.f
		
		//2: foo(u)
		// 3: u.f = ... 
		addFieldPush(node(0,"c"), f("g"), node(1,"a"));
		addCallFlow(node(1,"a"), node(2,"u"),returnSite(4));
		addFieldPush(node(2,"u"), f("f"), node(3,"u"));
		addReturnFlow(node(3,"u"),var("a"));
		addFieldPop(node(4,"a"), f("f"), node(5,"e"));
		addCallFlow(node(5,"e"), node(2,"u"), returnSite(6));
		addReturnFlow(node(3,"u"),var("e"));
		addFieldPop(node(6,"e"), f("f"), node(7,"h"));
		solver.solve(node(0,"c"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(7,"h")));
	}
	
	@Test
	public void positiveNoFieldsSummaryTest() {
//		1 :a.g = c
//		4: foo(a)
//		5: e = a.f
//		6: foo(e)
//		7: h = e.f
		
		//2: foo(u)
		// 3: u.f = ... 
		addNormal(node(0,"c"), node(1,"a"));
		addCallFlow(node(1,"a"), node(2,"u"),returnSite(4));
		addNormal(node(2,"u"),  node(3,"u"));
		addReturnFlow(node(3,"u"),var("a"));
		addNormal(node(4,"a"),  node(5,"e"));
		addCallFlow(node(5,"e"), node(2,"u"), returnSite(6));
		addReturnFlow(node(3,"u"),var("e"));
		addNormal(node(6,"e"),  node(7,"h"));
		solver.solve(node(0,"c"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(7,"h")));
	}
	
	@Test
	public void positiveSummaryFlowTest() {
		addCallFlow(node(1,"a"), node(2,"u"),returnSite(4));
		addReturnFlow(node(2,"u"),var("e"));
		addCallFlow(node(4,"e"), node(2,"u"), returnSite(6));
		addReturnFlow(node(2,"u"),var("e"));
		solver.solve(node(1,"a"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(6,"e")));
	}
	
	@Test
	public void negativeTestFieldPushAndPop() {
		addFieldPush(node(1,"u"), f("h"), node(2,"v"));
		addFieldPop(node(2,"v"), f("f"), node(3,"w"));
		solver.solve(node(1,"u"));
		assertFalse(solver.getReachedStates().contains(node(3,"w")));
	}
	@Test
	public void negativeTestCallSitePushAndPop() {
		addCallFlow(node(1,"u"), node(2,"v"),returnSite(4));
		addReturnFlow(node(2,"v"),var("w"));
		addNormal(node(3,"w"), node(4,"w"));
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(node(3,"w")));
	}
	
	@Test
	public void positiveTestCallSitePushAndPop() {
		addCallFlow(node(1,"u"), node(4,"v"),returnSite(2));
		addReturnFlow(node(4,"v"), var("w"));
		addNormal(node(2,"w"), node(3,"w"));
		solver.solve(node(1,"u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(node(3,"w")));
	}
	
	
	
	private Variable var(String v) {
		return new Variable(v);
	}

	private static Statement returnSite(int call) {
		return new Statement(call);
	}

	private static FieldRef f(String f) {
		return new FieldRef(f);
	}

	public static Node<Statement,Variable> node(int stmt,String var){
		return new Node<Statement,Variable>(new Statement(stmt),new Variable(var));
	}

	private static class Statement extends StringBasedObj implements Location {
		public Statement(int name) {
			super(Integer.toString(name));
		}
	}

	private static class Variable extends StringBasedObj {
		public Variable(String name) {
			super(name);
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
