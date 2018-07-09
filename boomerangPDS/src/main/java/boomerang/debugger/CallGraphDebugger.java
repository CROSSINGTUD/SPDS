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

/**
 * Can be used to obtain a dot file which can be plotted into a graphical representation of the call graph.
 * Call graph includes all edges and all methods which have edges incoming or outgoing.
 */
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

        //Use string builder to get text for call graph
        StringBuilder stringBuilder = new StringBuilder();

        //Needed to make graph in dot
        stringBuilder.append("digraph callgraph { \n");
        stringBuilder.append("node [margin=0, shape=box]; \n");

        //Add content of graph
        addMethodsToDotfile(stringBuilder);

        //End graph
        stringBuilder.append("}");

        //Write out what was gathered in the string builder
        try (FileWriter file = new FileWriter(dotFile)) {
            logger.info("Writing visualization to file {}", dotFile.getAbsolutePath());
            file.write(stringBuilder.toString());
            logger.info("Visualization available in file {}", dotFile.getAbsolutePath());
        } catch (IOException e) {
            logger.info("Exception in writing to visualization file {} : {}", dotFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Add all edges to string builder. The nodes between which edges run will be included, other
     * methods will not.
     */
    private void addMethodsToDotfile(StringBuilder stringBuilder) {
        for (Edge edge : callGraph) {
            addMethodToDotFile(stringBuilder, edge.src());
            stringBuilder.append(" -> ");
            addMethodToDotFile(stringBuilder, edge.tgt());
            stringBuilder.append("; \n");
        }
    }

    /**
     * Appends escaped method name to string builder, otherwise symbols like spaces
     * mess with the dot syntax
     */
    private void addMethodToDotFile(StringBuilder stringBuilder, SootMethod method){
        stringBuilder.append('"');
        stringBuilder.append(method);
        stringBuilder.append('"');
    }
}
