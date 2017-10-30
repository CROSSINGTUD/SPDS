package wpds.impl;

import java.util.Map;

import com.google.common.collect.Maps;

import wpds.interfaces.Location;
import wpds.interfaces.State;

public class SummaryNestedWeightedPAutomatons<N extends Location, D extends State, W extends Weight> implements NestedWeightedPAutomatons<N, D, W> {

	private Map<D, WeightedPAutomaton<N, D, W>> summaries = Maps.newHashMap();
	@Override
	public void putSummaryAutomaton(D target, WeightedPAutomaton<N, D, W> aut) {
		summaries.put(target, aut);
	}

	@Override
	public WeightedPAutomaton<N, D, W> getSummaryAutomaton(D target) {
		return summaries.get(target);
	}

}
