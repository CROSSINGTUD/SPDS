package boomerang.callgraph;

import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class BoomerangICFG extends JimpleBasedInterproceduralCFG {
	public BoomerangICFG(boolean enableException) {
		super(enableException);
		this.includePhantomCallees = true;
	}
}
