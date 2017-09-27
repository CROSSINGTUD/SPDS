package typestate;

import java.util.Iterator;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import boomerang.accessgraph.AccessGraph;
import soot.SootMethod;

public class ResultCollection<V> implements Iterable<Cell<SootMethod, AccessGraph, V>> {
	private Table<SootMethod, AccessGraph, V> resultTable = HashBasedTable.create();
	private Join<V> join;

	public ResultCollection(Join<V> join){
		this.join = join;
	}
	public void put(SootMethod m, AccessGraph g, V v) {
		if (v == null)
			throw new RuntimeException("Value v must not be null");
		V v2 = resultTable.get(m, g);
		if (v2 != null)
			v = join.join(v,v2);
		resultTable.put(m, g, v);
	}


	public void clear() {
		resultTable.clear();
	}

	@Override
	public Iterator<Cell<SootMethod, AccessGraph, V>> iterator() {
		return resultTable.cellSet().iterator();
	}

	public int size() {
		return resultTable.size();
	}
}
