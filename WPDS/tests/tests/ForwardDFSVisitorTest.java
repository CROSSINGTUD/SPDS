package tests;

import static tests.TestHelper.a;
import static tests.TestHelper.s;
import static tests.TestHelper.t;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import wpds.impl.PAutomaton;
import wpds.impl.Transition;
import wpds.interfaces.ReachabilityListener;

public class ForwardDFSVisitorTest {
	PAutomaton<StackSymbol, Abstraction> fa = new PAutomaton<StackSymbol, Abstraction>() {
		@Override
		public Abstraction createState(Abstraction d, StackSymbol loc) {
			return new Abstraction(d, loc);
		}

		@Override
		public StackSymbol epsilon() {
			return s("EPS");
		}
	};
	final Set<Transition<StackSymbol, Abstraction>> reachables = Sets.newHashSet();
	@Test
	public void delayedAdd() {
		fa.registerDFSListener(a(0),
				new ReachabilityListener<StackSymbol, Abstraction>() {
					@Override
					public void reachable(Transition<StackSymbol, Abstraction> t) {
						reachables.add(t);
					}
				});
		fa.addTransition(t(0, "n1", 1));
		Assert.assertFalse(reachables.isEmpty());
		Assert.assertTrue(reachableMinusTrans().isEmpty());
		fa.addTransition(t(1, "n1", 2));
		Assert.assertTrue(reachableMinusTrans().isEmpty());
	}
	
	
	@Test
	public void delayedAddListener() {
		fa.addTransition(t(0, "n1", 1));
		fa.addTransition(t(1, "n1", 2));
		Assert.assertFalse(fa.getTransitions().isEmpty());
		fa.registerDFSListener(a(0),
				new ReachabilityListener<StackSymbol, Abstraction>() {
					@Override
					public void reachable(Transition<StackSymbol, Abstraction> t) {
						System.out.println("Reachable" + t);
						reachables.add(t);
					}
				});
		Assert.assertFalse(reachables.isEmpty());
		Assert.assertTrue(reachableMinusTrans().isEmpty());

		fa.addTransition(t(4, "n1", 5));
		Assert.assertTrue(fa.getTransitions().size() > reachables.size());
		

		fa.addTransition(t(2, "n1", 5));
		Assert.assertTrue(fa.getTransitions().size() > reachables.size());
		Assert.assertFalse(reachableMinusTrans().isEmpty());
		

		fa.addTransition(t(2, "n1", 4));
		Assert.assertTrue(fa.getTransitions().size() == reachables.size());
		Assert.assertTrue(reachableMinusTrans().isEmpty());

		fa.addTransition(t(3, "n1", 8));
		fa.addTransition(t(8, "n1", 9));
		fa.addTransition(t(3, "n1", 7));
		fa.addTransition(t(3, "n1", 6));
		fa.addTransition(t(6, "n1", 3));
		Assert.assertTrue(fa.getTransitions().size() > reachables.size());
		Assert.assertFalse(reachableMinusTrans().isEmpty());
		
		
		fa.addTransition(t(1, "n1", 3));
		Assert.assertTrue(reachableMinusTrans().isEmpty());
	}


	private Set<Transition<StackSymbol, Abstraction>> reachableMinusTrans() {
		HashSet<Transition<StackSymbol, Abstraction>> res = Sets.newHashSet(fa.getTransitions());
		res.removeAll(reachables);
		return res;
	}
}
