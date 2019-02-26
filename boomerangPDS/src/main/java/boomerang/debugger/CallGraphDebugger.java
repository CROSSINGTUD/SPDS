package boomerang.debugger;

import boomerang.Query;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.solver.AbstractBoomerangSolver;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import soot.Kind;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import wpds.impl.Weight;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Can be used to obtain a dot file which can be plotted into a graphical representation of the call graph. Call graph
 * includes all edges and all methods which have edges incoming or outgoing.
 */
public class CallGraphDebugger<W extends Weight> extends Debugger<W> {

    private static final Logger logger = LogManager.getLogger();

    private File dotFile;
    private ObservableICFG<Unit, SootMethod> icfg;
    private CallGraph callGraph;

    private HashSet<Unit> totalCallSites = new HashSet<>();
    private Multimap<Unit, SootMethod> virtualCallSites = HashMultimap.create();
    private int numVirtualCallSites;
    private int numVirtualCallSitesSingleTarget;
    private int numVirtualCallSitesMultipleTarget;
    private float avgNumTargetsVirtualCallSites;
    private float avgNumTargetMultiTargetCallSites;
    private Multimap<SootMethod, Unit> predecessors = HashMultimap.create();
    private float avgNumOfPredecessors;
    private int numOfEdgesInCallGraph;
    private int numEdgesFromPrecomputed;
    private static int seedNumber = 0;

    public CallGraphDebugger(File dotFile, ObservableICFG<Unit, SootMethod> icfg) {
        this.dotFile = dotFile;
        this.icfg = icfg;
    }

    @Override
    public void done(Map<Query, AbstractBoomerangSolver<W>> queryToSolvers) {
        callGraph = icfg.getCallGraphCopy();

        // Check if we have a static icfg, so the call graph does not change across seeds
        if (icfg instanceof ObservableStaticICFG) {
            // Check if we already have made a dot file for that icfg once
            if (dotFile.exists()) {
                // Then we do not need to do it again
                return;
            }
        } else {
            // We have a dynamic icfg that is different for every seed. Enumerate the files
            seedNumber++;
            // The call graph debugger becomes active for both phases of the IDEALSeedSolver, and they operate on the
            // same call graph so do not output that
            // call graph twice
            if (seedNumber % 2 == 0) {
                return;
            }
            int actualSeedNumber = seedNumber / 2 + 1;
            String dotFileName = dotFile.getAbsolutePath();
            dotFileName = dotFileName.substring(0, dotFileName.lastIndexOf('.')) + actualSeedNumber + ".dot";
            dotFile = new File(dotFileName);
        }

        logger.info("Starting to compute visualization.");

        // Use string builder to get text for call graph
        StringBuilder stringBuilder = new StringBuilder();

        // Needed to make graph in dot
        stringBuilder.append("digraph callgraph { \n");
        stringBuilder.append("node [margin=0, shape=box]; \n");

        // Add content of graph
        addMethodsToDotfile(stringBuilder);

        // End graph
        stringBuilder.append("}");

        // Write out what was gathered in the string builder
        try (FileWriter file = new FileWriter(dotFile)) {
            logger.trace("Writing visualization to file {}", dotFile.getAbsolutePath());
            file.write(stringBuilder.toString());
            logger.info("Visualization available in file {}", dotFile.getAbsolutePath());
        } catch (IOException e) {
            logger.info("Exception in writing to visualization file {} : {}", dotFile.getAbsolutePath(),
                    e.getMessage());
        }
    }

    /**
     * Add all edges to string builder. The nodes between which edges run will be included, other methods will not.
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
     * Appends escaped method name to string builder, otherwise symbols like spaces mess with the dot syntax
     */
    private void addMethodToDotFile(StringBuilder stringBuilder, SootMethod method) {
        stringBuilder.append('"');
        stringBuilder.append(method);
        stringBuilder.append('"');
    }

    private void computeCallGraphStatistics() {
        numOfEdgesInCallGraph = callGraph.size();
        for (Edge edge : callGraph) {
            Unit srcUnit = edge.srcUnit();
            totalCallSites.add(srcUnit);
            if (edge.kind().equals(Kind.VIRTUAL)) {
                virtualCallSites.put(srcUnit, edge.tgt());
                predecessors.put(edge.tgt(), srcUnit);
            }
        }
        computeVirtualCallSiteMetrics();
        computePredecessorMetrics();
        if (icfg != null) {
            numEdgesFromPrecomputed = icfg.getNumberOfEdgesTakenFromPrecomputedGraph();
        }
        if (numEdgesFromPrecomputed < 0) {
            numEdgesFromPrecomputed = numOfEdgesInCallGraph;
        }
    }

    private void computeVirtualCallSiteMetrics() {
        numVirtualCallSites = virtualCallSites.keySet().size();
        int totalTargetsVirtualCallSites = 0;
        for (Map.Entry<Unit, Collection<SootMethod>> entry : virtualCallSites.asMap().entrySet()) {
            int targets = entry.getValue().size();
            if (targets > 1) {
                numVirtualCallSitesMultipleTarget++;
            } else if (targets == 1) {
                numVirtualCallSitesSingleTarget++;
            }
            totalTargetsVirtualCallSites += targets;
        }
        avgNumTargetsVirtualCallSites = totalTargetsVirtualCallSites / (float) numVirtualCallSites;
        avgNumTargetMultiTargetCallSites = totalTargetsVirtualCallSites / (float) numVirtualCallSitesMultipleTarget;
    }

    private void computePredecessorMetrics() {
        int numMethods = predecessors.keySet().size();
        int totalPredecessors = 0;
        for (Map.Entry<SootMethod, Collection<Unit>> entry : predecessors.asMap().entrySet()) {
            totalPredecessors += entry.getValue().size();
        }
        avgNumOfPredecessors = totalPredecessors / (float) numMethods;
    }

    public String getCsvHeader() {
        return "numOfEdgesInCallGraph; totalCallSites; "
                + "virtualCallSites; virtualCallSitesSingleTarget; virtualCallSitesMultipleTarget; "
                + "avgNumTargetsVirtualCallSites; avgNumTargetMultiTargetCallSites;"
                + "avgNumOfPredecessors; edgesFromPrecomputed;";
    }

    public String getCallGraphStatisticsAsCsv() {
        computeCallGraphStatistics();
        return String.valueOf(numOfEdgesInCallGraph) + ';' + totalCallSites.size() + ';' + numVirtualCallSites + ';'
                + numVirtualCallSitesSingleTarget + ';' + numVirtualCallSitesMultipleTarget + ';'
                + avgNumTargetsVirtualCallSites + ';' + avgNumTargetMultiTargetCallSites + ';' + avgNumOfPredecessors
                + ';' + numEdgesFromPrecomputed + ';';
    }

}
