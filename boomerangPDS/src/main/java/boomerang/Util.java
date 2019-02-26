package boomerang;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.queue.QueueReader;

public class Util {
    private static int icfgEdges;

    public static boolean isParameterLocal(Val val, SootMethod m) {
        if (val.isStatic())
            return false;
        if (!m.hasActiveBody()) {
            throw new RuntimeException("Soot Method has no active body");
        }
        List<Local> parameterLocals = m.getActiveBody().getParameterLocals();
        return parameterLocals.contains(val.value());
    }

    public static boolean isReturnOperator(Val val, Statement returnStmt) {
        Stmt stmt = returnStmt.getUnit().get();
        return (stmt instanceof ReturnStmt && ((ReturnStmt) stmt).getOp().equals(val.value()));
    }

    public static boolean isThisLocal(Val val, SootMethod m) {
        if (val.isStatic())
            return false;
        if (!m.hasActiveBody()) {
            throw new RuntimeException("Soot Method has no active body");
        }
        if (m.isStatic())
            return false;
        Local parameterLocals = m.getActiveBody().getThisLocal();
        return parameterLocals.equals(val.value());
    }

    private static long getGcCount() {
        long sum = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = b.getCollectionCount();
            if (count != -1) {
                sum += count;
            }
        }
        return sum;
    }

    public static long getICFGEdges() {
        if (icfgEdges > 0)
            return icfgEdges;
        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
        while (listener.hasNext()) {
            MethodOrMethodContext next = listener.next();
            SootMethod method = next.method();
            if (!method.hasActiveBody())
                continue;
            Body activeBody = method.getActiveBody();
            for (Unit u : activeBody.getUnits()) {
                List<Unit> succsOf = icfg.getSuccsOf(u);
                icfgEdges += succsOf.size();
                if (icfg.isCallStmt(u)) {
                    icfgEdges += icfg.getCalleesOfCallAt(u).size();
                }
                if (icfg.isExitStmt(u)) {
                    icfgEdges += icfg.getCallersOf(method).size();
                }
            }
        }
        return icfgEdges;
    }

    public static long getReallyUsedMemory() {
        return 0;
        // long before = getGcCount();
        // System.gc();
        // while (getGcCount() == before)
        // ;
        // return getCurrentlyUsedMemory();
    }

    private static long getCurrentlyUsedMemory() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        // + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
    }

}
