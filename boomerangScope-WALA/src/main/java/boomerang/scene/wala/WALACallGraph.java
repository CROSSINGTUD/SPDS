/**
 * ***************************************************************************** Copyright (c) 2020
 * CodeShield GmbH, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene.wala;

import boomerang.scene.CallGraph;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WALACallGraph extends CallGraph {

  private Map<IMethod, WALAMethod> iMethodToWALAMethod = new HashMap<>();
  private IClassHierarchy cha;

  public WALACallGraph(com.ibm.wala.ipa.callgraph.CallGraph cg, IClassHierarchy cha) {
    this.cha = cha;
    Collection<CGNode> ep = cg.getEntrypointNodes();
    Set<WALAMethod> visited = Sets.newHashSet();
    LinkedList<CGNode> worklist = Lists.newLinkedList();
    for (CGNode e : ep) {
      worklist.add(e);
      this.addEntryPoint(getOrCreate(e));
    }

    Stopwatch watch = Stopwatch.createStarted();
    Stopwatch irw = Stopwatch.createUnstarted();
    while (!worklist.isEmpty()) {
      CGNode curr = worklist.poll();
      if (ignore(curr.getMethod())) continue;
      if (!visited.add(getOrCreate(curr))) continue;
      Iterator<CGNode> succNodes = cg.getSuccNodes(curr);
      while (succNodes.hasNext()) {
        CGNode succ = succNodes.next();
        irw.start();
        irw.stop();
        //				if(visited.size() % 100 == 0) {
        //					System.out.println("Total Time:" +watch.elapsed(TimeUnit.SECONDS));
        //					System.out.println("IR time: " + irw.elapsed(TimeUnit.SECONDS));
        //					System.out.println(visited.size());
        //					System.out.println(worklist.size());
        //				}
        worklist.add(succ);
        Iterator<CallSiteReference> callSites = cg.getPossibleSites(curr, succ);
        while (callSites.hasNext()) {
          CallSiteReference ref = callSites.next();
          SSAAbstractInvokeInstruction[] calls = curr.getIR().getCalls(ref);
          for (SSAAbstractInvokeInstruction i : calls) {
            if (ignore(succ.getMethod())) {
              continue;
            }
            this.addEdge(new Edge(new WALAStatement(i, getOrCreate(curr)), getOrCreate(succ)));
          }
        }
      }
    }
    System.out.println("Edges:" + size());
    System.out.println("Total Time:" + watch.elapsed(TimeUnit.SECONDS));
    System.out.println("IR time: " + irw.elapsed(TimeUnit.SECONDS));
  }

  private WALAMethod getOrCreate(CGNode curr) {
    IMethod method = curr.getMethod();
    WALAMethod walaMethod = iMethodToWALAMethod.get(method);
    if (walaMethod != null) return walaMethod;
    WALAMethod m = new WALAMethod(curr.getMethod(), curr.getIR(), cha);
    iMethodToWALAMethod.put(method, m);
    return m;
  }

  private boolean ignore(IMethod method) {
    return method.isBridge()
        || method.isClinit()
        || method.isNative()
        || method.isSynthetic()
        || method.isWalaSynthetic();
  }
}
