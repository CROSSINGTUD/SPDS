package boomerang;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public class Util {
  private static int icfgEdges;

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

  //    public static long getICFGEdges() {
  //        if (icfgEdges > 0)
  //            return icfgEdges;
  //        ReachableMethods reachableMethods = Scene.v().getReachableMethods();
  //        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
  //        QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
  //        while (listener.hasNext()) {
  //            MethodOrMethodContext next = listener.next();
  //            SootMethod method = next.method();
  //            if (!method.hasActiveBody())
  //                continue;
  //            Body activeBody = method.getActiveBody();
  //            for (Unit u : activeBody.getUnits()) {
  //                List<Unit> succsOf = icfg.getSuccsOf(u);
  //                icfgEdges += succsOf.size();
  //                if (icfg.isCallStmt(u)) {
  //                    icfgEdges += icfg.getCalleesOfCallAt(u).size();
  //                }
  //                if (icfg.isExitStmt(u)) {
  //                    icfgEdges += icfg.getCallersOf(method).size();
  //                }
  //            }
  //        }
  //        return icfgEdges;
  //    }

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
