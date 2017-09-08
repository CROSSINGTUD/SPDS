package tests;

import static tests.TestHelper.ACC;
import static tests.TestHelper.accepts;
import static tests.TestHelper.t;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import tests.TestHelper.Abstraction;
import tests.TestHelper.StackSymbol;
import static tests.TestHelper.*;
import wpds.impl.PAutomaton;
import wpds.impl.Transition;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.ForwardDFSVisitor;
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
	@Before
	public void before(){
		fa.registerListener(new ForwardDFSVisitor<StackSymbol, Abstraction, NoWeight<StackSymbol>>(fa, a(0),
				new ReachabilityListener<StackSymbol, Abstraction>() {
					@Override
					public void reachable(Transition<StackSymbol, Abstraction> t) {
						reachables.add(t);
					}
				}));
	}
	@Test
	public void delayedAdd() {
		fa.addTransition(t(0, "n1", 1));
		Assert.assertFalse(reachables.isEmpty());
		reachables.removeAll(fa.getTransitions());
		Assert.assertTrue(reachables.isEmpty());
		fa.addTransition(t(1, "n1", 2));
		reachables.removeAll(fa.getTransitions());
		Assert.assertTrue(reachables.isEmpty());
	}

}
