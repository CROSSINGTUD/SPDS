package boomerang.debugger;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import heros.debug.visualization.ExplodedSuperGraph;
import heros.debug.visualization.IDEToJSON;
import heros.debug.visualization.ExplodedSuperGraph.ESGNode;
import heros.debug.visualization.IDEToJSON.Direction;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.Node;
import wpds.impl.Transition;
import wpds.impl.Weight;

public class IDEVizDebugger<W extends Weight> extends Debugger<W>{

	private File ideVizFile;
//	private IDEToJSON<SootMethod, Unit, Value, Object, InterproceduralCFG<Unit,SootMethod>> ideToJson;
	private InterproceduralCFG<Unit, SootMethod> icfg;


	public IDEVizDebugger(File ideVizFile, InterproceduralCFG<Unit, SootMethod> icfg) {
		this.ideVizFile = ideVizFile;
		this.icfg = icfg;
	}

	@Override
	public void reachableNodes(Query q, Map<Transition<Statement, INode<Val>>, W> reachedStates) {
		System.out.println(ideVizFile.getAbsolutePath() +q.toString().replace(" ",""));
		IDEToJSON<SootMethod, Unit, Val, Object, InterproceduralCFG<Unit, SootMethod>> ideToJson = new IDEToJSON<SootMethod, Unit, Val, Object, InterproceduralCFG<Unit,SootMethod>>(new File(ideVizFile.getAbsolutePath() +q.toString().replace(" ","").replace(":", "").replaceAll("<", "").replace(">", "")), icfg);
		for(Entry<Transition<Statement, INode<Val>>, W> e : reachedStates.entrySet()){
			Transition<Statement, INode<Val>> key = e.getKey();
			Statement stmt = key.getLabel();
			INode<Val> fact = key.getStart();
			if(stmt.getMethod() == null)
				continue;
			ExplodedSuperGraph<SootMethod, Unit, Val, Object> esg = ideToJson.getOrCreateESG(stmt.getMethod(), (q instanceof ForwardQuery ? Direction.Forward : Direction.Backward));
			if(stmt.getUnit().isPresent()){
				Stmt start = stmt.getUnit().get();
				esg.normalFlow(start, fact.fact(), start, fact.fact());
				esg.setValue(esg.new ESGNode(start, fact.fact()),e.getValue());
			}
		}
		ideToJson.writeToFile();
		super.reachableNodes(q, reachedStates);
	}
}
