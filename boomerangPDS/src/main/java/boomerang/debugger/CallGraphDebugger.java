package boomerang.debugger;

import boomerang.Query;
import boomerang.solver.AbstractBoomerangSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import wpds.impl.Weight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class CallGraphDebugger<W extends Weight> extends Debugger<W>{

    private static final Logger logger = LogManager.getLogger();
    
    private File dotFile;
    private CallGraph callGraph;
    
    public CallGraphDebugger(File dotFile, CallGraph callGraph){
        this.dotFile = dotFile;
        this.callGraph = callGraph;
    }
    
    @Override
    public void done(Map<Query, AbstractBoomerangSolver<W>> queryToSolvers) {
        logger.info("Starting to compute visualization.");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("digraph callgraph {");
        addMethodsToDotfile(stringBuilder);
        stringBuilder.append("}");

        try (FileWriter file = new FileWriter(dotFile)) {
            logger.info("Writing visualization to file {}", dotFile.getAbsolutePath());
            file.write(stringBuilder.toString());
            logger.info("Visualization available in file {}", dotFile.getAbsolutePath());
        } catch (IOException e) {
            logger.info("Exception in writing to visualization file {} : {}", dotFile.getAbsolutePath(), e.getMessage());
        }
    }

    private void addMethodsToDotfile(StringBuilder stringBuilder) {
        //Add connections if present
        for (Edge edge : callGraph) {
            addMethodToDotFile(stringBuilder, edge.src());
            stringBuilder.append(" -> ");
            addMethodToDotFile(stringBuilder, edge.tgt());
            stringBuilder.append("; \n");
        }
    }

    private void addMethodToDotFile(StringBuilder stringBuilder, SootMethod method){
        stringBuilder.append('"');
        stringBuilder.append(method);
        stringBuilder.append('"');
    }
}
