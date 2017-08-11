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
import analysis.SingleNode;
import analysis.Solver;
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
		addSucc(curr, succ);
		addFieldFlow(asField(curr), asField(succ), new UPopRule<FieldRef, NodeWithLocation<Statement, Variable,FieldRef>>(asField(curr), pop, asField(succ)));
		addFlow(curr, succ, new UNormalRule<Statement, INode<Variable>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt()));
	}

	private void addFieldPush(Node<Statement, Variable> curr, FieldRef push, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
		addFieldFlow(asField(curr), asField(succ), new UPushRule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard(),push));
		addFlow(curr, succ, new UNormalRule<Statement, INode<Variable>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt()));
	}


	private void addNormal(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
		addFieldFlow(asField(curr), asField(succ), new UNormalRule<FieldRef, NodeWithLocation<Statement, Variable,FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard()));
		addFlow(curr, succ, new UNormalRule<Statement, INode<Variable>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt()));
	}
	private void addFieldNormal(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
		addFieldFlow(asField(curr), asField(succ), new UNormalRule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>>(asField(curr), new FieldWildCard(), asField(succ),new FieldWildCard()));
	}

	private void addFieldFlow(NodeWithLocation<Statement, Variable, FieldRef> curr,
			NodeWithLocation<Statement, Variable, FieldRef> succ,
			Rule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>, NoWeight<FieldRef>> rule) {
		Collection<Rule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>, NoWeight<FieldRef>>> collection = fieldRefRuleMap.get(curr, succ);
		if(collection == null)
			collection = Sets.newHashSet();
		collection.add(rule);
		fieldRefRuleMap.put(curr, succ, collection);
	}

	private void addReturnFlow(Node<Statement, Variable> curr, Statement pop, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
		addFlow(curr, succ, new UPopRule<Statement, INode<Variable>>(wrap(curr.fact()), pop, wrap(succ.fact())));
		addFieldNormal(curr, succ);
	}
	
	public void addFlow(Node<Statement, Variable> curr, Node<Statement, Variable> succ, Rule<Statement, INode<Variable>, NoWeight<Statement>> rule){
		Collection<Rule<Statement, INode<Variable>, NoWeight<Statement>>> set = callSiteRuleMap.get(curr, succ);
		if(set == null)
			set = Sets.newHashSet();
		set.add(rule);
		callSiteRuleMap.put(curr, succ, set);
	}

	private void addCallFlow(Node<Statement, Variable> curr, Node<Statement, Variable> succ, Statement calleeStart) {
		addSucc(curr, succ);
		addFlow(curr, succ, new UPushRule<Statement, INode<Variable>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), calleeStart,succ.stmt()));
		addFieldNormal(curr, succ);
	} 
	private void addNormalFlow(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		addSucc(curr, succ);
		addFlow(curr, succ, new UNormalRule<Statement, INode<Variable>>(wrap(curr.fact()), curr.stmt(), wrap(succ.fact()), succ.stmt()));
		addFieldNormal(curr, succ);
	}
	private void addSucc(Node<Statement, Variable> curr, Node<Statement, Variable> succ) {
		successorMap.put(curr, succ);
	}
	private FieldRef epsilonField = new FieldRef("eps_f");
	private Statement epsilonCallSite = new Statement("eps_c");
	
	private INode<Variable> wrap(Variable variable) {
		return new SingleNode<Variable>(variable);
	}
	

	private NodeWithLocation<Statement, Variable, FieldRef> asField(Node<Statement,Variable> node){
		return new NodeWithLocation<Statement, Variable, FieldRef>(node.stmt(), node.fact(), solver.emptyField());
	}
	private Solver<Statement, Variable, FieldRef> solver = new Solver<Statement, Variable, FieldRef>() {
		@Override
		public Collection<Rule<Statement, INode<Variable>, NoWeight<Statement>>> computeCallSiteRule(Node<Statement, Variable> curr,
				Node<Statement, Variable> succ) {
			 Collection<Rule<Statement, INode<Variable>, NoWeight<Statement>>> res = callSiteRuleMap.get(curr, succ);
			 if(res == null)
				 return Sets.newHashSet();
			return res;
		}

		@Override
		public Collection<Rule<FieldRef, NodeWithLocation<Statement, Variable, FieldRef>, NoWeight<FieldRef>>> computeFieldRefRule(NodeWithLocation<Statement, Variable, FieldRef> curr,
				NodeWithLocation<Statement, Variable, FieldRef> succ) {
			return fieldRefRuleMap.get(curr, succ);
		}

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
	};

	@Test
	public void test1() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addCallFlow(s("2","v"), s("3","p"),call("foo"));
		addFieldPush(s("3","p"), f("g"), s("4","q"));
		addReturnFlow(s("4","q"), call("foo"), s("5","w"));
		addFieldPop(s("5","w"), f("g"), s("6","x"));
		addFieldPop(s("6","x"), f("f"), s("7","y"));
		
//		second branch
		addFieldPush(s("8","r"), f("f"), s("9","s"));
		addCallFlow(s("9","s"),s("3","p"),call("foo"));
		addReturnFlow(s("4","q"), call("foo"), s("10","t"));
		addFieldPush(s("10","t"), f("f"), s("11","s"));
		
		solver.solve(s("1","u"));
		System.out.println(solver.getReachedStates());
		assertFalse(solver.getReachedStates().contains(s("10","t")));
		assertTrue(solver.getReachedStates().contains(s("5","w")));
		assertTrue(solver.getReachedStates().contains(s("6","x")));
		assertFalse(solver.getReachedStates().contains(s("7","y")));
	}
	
	@Test
	public void simple() {
		addNormalFlow(s("1","v"), s("2","w"));
		addCallFlow(s("2","w"), s("3","p"),call("4"));
		addNormalFlow(s("4","p"),  s("5","q"));
		addNormalFlow(s("5","q"),  s("6","x"));
		addReturnFlow(s("6","x"), call("4"),s("6","p"));
		
		solver.solve(s("1","v"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(s("6","p")));
	}
	
	
	@Test
	public void testWithTwoStacks() {
		addFieldPush(s("1","u"), f("h"), s("2","v"));
		addCallFlow(s("2","v"),  s("3","p"), call("4"));
		addFieldPush(s("4","p"), f("g"), s("5","q"));
		addReturnFlow(s("5","q"), call("5"), s("3","w"));
		addNormal(s("3","w"),s("7","t"));
		solver.solve(s("1","u"));
		System.out.println(solver.getReachedStates());
		assertTrue(solver.getReachedStates().contains(s("7","t")));
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

	public static Node<Statement,Variable> s(String stmt,String var){
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
