package boomerang.debugger;

import java.io.File;
import java.util.Set;

import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import heros.InterproceduralCFG;
import heros.debug.visualization.ExplodedSuperGraph;
import heros.debug.visualization.IDEToJSON;
import heros.debug.visualization.IDEToJSON.Direction;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.Node;

public class IDEVizDebugger extends Debugger{

	private File ideVizFile;
//	private IDEToJSON<SootMethod, Unit, Value, Object, InterproceduralCFG<Unit,SootMethod>> ideToJson;
	private InterproceduralCFG<Unit, SootMethod> icfg;


	public IDEVizDebugger(File ideVizFile, InterproceduralCFG<Unit, SootMethod> icfg) {
		this.ideVizFile = ideVizFile;
		this.icfg = icfg;
	}

	@Override
	public void reachableNodes(Query q, Set<Node<Statement, Val>> reachedStates) {
		System.out.println(ideVizFile.getAbsolutePath() +q.toString().replace(" ",""));
		IDEToJSON<SootMethod, Unit, Value, Object, InterproceduralCFG<Unit, SootMethod>> ideToJson = new IDEToJSON<SootMethod, Unit, Value, Object, InterproceduralCFG<Unit,SootMethod>>(new File(ideVizFile.getAbsolutePath() +q.toString().replace(" ","").replace(":", "").replaceAll("<", "").replace(">", "")), icfg);
		for(Node<Statement,Val> states : reachedStates){
			ExplodedSuperGraph<SootMethod, Unit, Value, Object> esg = ideToJson.getOrCreateESG(states.stmt().getMethod(), (q instanceof ForwardQuery ? Direction.Forward : Direction.Backward));
			Value startFact = states.fact().value();
			if(states.stmt().getUnit().isPresent()){
				Stmt start = states.stmt().getUnit().get();
				esg.normalFlow(start, startFact, start, startFact);
			}
		}
		ideToJson.writeToFile();
		super.reachableNodes(q, reachedStates);
	}
}
