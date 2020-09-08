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

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WALAControlFlowGraph implements ControlFlowGraph {

  private Method method;
  private SSACFG cfg;
  private IClassHierarchy cha;
  private Map<ISSABasicBlock, Statement> basicBlockToFirstStmt = Maps.newHashMap();

  public WALAControlFlowGraph(WALAMethod method, IClassHierarchy cha) {
    this.method = method;
    this.cha = cha;
    this.cfg = method.getIR().getControlFlowGraph();
    buildCache();
  }

  private boolean cacheBuild = false;
  private List<Statement> startPointCache = Lists.newArrayList();
  private List<Statement> endPointCache = Lists.newArrayList();
  private Multimap<Statement, Statement> succsOfCache = HashMultimap.create();
  private Multimap<Statement, Statement> predsOfCache = HashMultimap.create();
  private List<Statement> statements = Lists.newArrayList();

  public Collection<Statement> getStartPoints() {
    buildCache();
    return startPointCache;
  }

  private void buildCache() {
    if (cacheBuild) return;
    cacheBuild = true;

    Set<ISSABasicBlock> emptyBasicBlocks = Sets.newHashSet();
    Map<ISSABasicBlock, Statement> basicBlockToLastStmt = Maps.newHashMap();
    Iterator<ISSABasicBlock> bbIt = cfg.iterator();
    // Convert each basic block.
    while (bbIt.hasNext()) {
      ISSABasicBlock next = bbIt.next();
      BasicBlock n = (BasicBlock) next;
      List<SSAInstruction> allInstructions = n.getAllInstructions();
      List<Statement> allStatements = convert(allInstructions);
      Statement last = null;
      Statement first = null;
      for (Statement curr : allStatements) {
        statements.add(curr);
        if (last == null) {
          last = curr;
          first = curr;
        } else {
          succsOfCache.put(last, curr);
          predsOfCache.put(curr, last);
          last = curr;
        }
      }
      if (last == null) {
        emptyBasicBlocks.add(next);
      } else {
        if (first == null) {
          throw new RuntimeException("Unexpected behaviour!");
        }
        basicBlockToFirstStmt.put(next, first);
        basicBlockToLastStmt.put(next, last);
      }
    }

    Graph<ISSABasicBlock> bbGraph = buildDirectedGraph();
    for (ISSABasicBlock eBB : emptyBasicBlocks) {
      bbGraph.removeNode(eBB);
    }
    Set<ISSABasicBlock> visited = Sets.newHashSet();
    LinkedList<ISSABasicBlock> worklist = Lists.newLinkedList();
    worklist.addAll(bbGraph.entries);
    while (!worklist.isEmpty()) {
      ISSABasicBlock curr = worklist.poll();
      if (emptyBasicBlocks.contains(curr)) {
        throw new RuntimeException("Unexpected behaviour!");
      } else if (!basicBlockToFirstStmt.containsKey(curr)) {
        throw new RuntimeException("Unexpected behaviour!");
      } else if (!basicBlockToLastStmt.containsKey(curr)) {
        throw new RuntimeException("Unexpected behaviour!");
      }
      if (!visited.add(curr)) {
        continue;
      }
      Collection<ISSABasicBlock> succNodes = bbGraph.outEdges.get(curr);
      for (ISSABasicBlock next : succNodes) {
        if (emptyBasicBlocks.contains(next)) {
          throw new RuntimeException("Unexpected behaviour!");
        }
        Statement firstOfNextBB = basicBlockToFirstStmt.get(next);
        Statement lastOfPrevBB = basicBlockToLastStmt.get(curr);
        succsOfCache.put(lastOfPrevBB, firstOfNextBB);
        predsOfCache.put(firstOfNextBB, lastOfPrevBB);
        worklist.add(next);
      }
    }
    WALAStatement entryStatement =
        new WALAStatement("NOP", method) {
          @Override
          public int hashCode() {
            return System.identityHashCode(this);
          }

          @Override
          public boolean equals(Object obj) {
            return this == obj;
          }
        };
    List<Statement> entries = Lists.newArrayList();
    for (Statement s : statements) {
      if (predsOfCache.get(s).isEmpty()) {
        entries.add(s);
      }
    }
    statements.add(0, entryStatement);
    LinkedList<Statement> entryStatements = addUnitializedFields();

    if (!entryStatements.isEmpty()) {
      predsOfCache.put(entryStatements.getFirst(), entryStatement);
      succsOfCache.put(entryStatement, entryStatements.getLast());
      for (Statement s : entries) {
        predsOfCache.put(s, entryStatements.getLast());
        succsOfCache.put(entryStatements.getLast(), s);
      }
    } else {
      for (Statement s : entries) {
        predsOfCache.put(s, entryStatement);
        succsOfCache.put(entryStatement, s);
      }
    }

    for (Statement s : statements) {
      if (succsOfCache.get(s).isEmpty()) {
        endPointCache.add(s);
      }
      if (predsOfCache.get(s).isEmpty()) {
        startPointCache.add(s);
      }
    }
    for (Statement s : statements) {
      if (s.containsInvokeExpr()) {
        if (s instanceof WALAStatement) {
          throw new RuntimeException(
              "The statement must be split into CallSite and ReturnSiteStatement");
        }
      }
    }
    for (Statement s : succsOfCache.values()) {
      if (s.containsInvokeExpr()) {
        if (s instanceof WALAStatement) {
          throw new RuntimeException(
              "The statement must be split into CallSite and ReturnSiteStatement");
        }
      }
    }
  }

  private List<Statement> convert(List<SSAInstruction> allInstructions) {
    List<Statement> res = Lists.newArrayList();
    for (SSAInstruction ins : allInstructions) {
      res.addAll(convert(ins));
    }
    // TODO Auto-generated method stub
    return res;
  }

  private Collection<? extends Statement> convert(SSAInstruction ins) {
    List<Statement> res = Lists.newArrayList();
    WALAStatement curr = new WALAStatement(ins, method);
    // TODO for PhiNodes as well.
    if (curr.containsInvokeExpr()) {
      res.addAll(handleCallSite(curr));
    } else if (curr.isFieldStore() && curr.getRightOp().isNull()) {
      res.add(new WALADummyNullStatement(curr.getRightOp(), method));
      res.add(curr);
    } else {
      res.add(curr);
    }
    return res;
  }

  private LinkedList<Statement> addUnitializedFields() {
    LinkedList<Statement> uninitializedStatements = Lists.newLinkedList();
    if (!method.isConstructor()) return uninitializedStatements;
    TypeReference delegate = (TypeReference) method.getDeclaringClass().getDelegate();
    IClass c = cha.lookupClass(delegate);
    Collection<IField> allFields = c.getAllFields();
    List<FieldReference> allDeclaredFields = computeInitializedFields();
    for (IField f : allFields) {
      allDeclaredFields.add(f.getReference());
    }
    List<FieldReference> definedFields = computeInitializedFields();
    allDeclaredFields.removeAll(definedFields);
    for (FieldReference ref : allDeclaredFields) {
      WALADummyVal dummyVal = new WALADummyVal((WALAMethod) method);
      uninitializedStatements.add(new WALADummyNullStatement(dummyVal, method));
      uninitializedStatements.add(
          new WALAUnitializedFieldStatement(
              new WALAField(ref), (WALAMethod) method, method.getThisLocal(), dummyVal));
    }
    addListOfStmts(uninitializedStatements);
    return uninitializedStatements;
  }

  private List<FieldReference> computeInitializedFields() {
    List<FieldReference> refs = Lists.newArrayList();
    for (Statement s : statements) {
      if (s.isFieldStore()) {
        WALAStatement del = (WALAStatement) s;
        SSAPutInstruction ssaInstruction = (SSAPutInstruction) del.getSSAInstruction();
        FieldReference declaredFieldType = ssaInstruction.getDeclaredField();
        refs.add(declaredFieldType);
      }
    }
    return refs;
  }

  private void addListOfStmts(List<Statement> stmts) {
    Statement last = null;
    for (Statement curr : stmts) {
      statements.add(curr);
      if (last == null) {
        last = curr;
      } else {
        succsOfCache.put(last, curr);
        predsOfCache.put(curr, last);
        last = curr;
      }
    }
  }

  private LinkedList<Statement> handleCallSite(WALAStatement call) {
    LinkedList<Statement> res = Lists.newLinkedList();
    containsNullVariables(call, res);
    //      addListOfStmts(res);
    return res;
  }

  private void containsNullVariables(WALAStatement call, List<Statement> res) {
    List<Val> args = call.getInvokeExpr().getArgs();
    for (Val a : args) {
      if (a.isNull()) {
        res.add(new WALADummyNullStatement(a, call.getMethod()));
      }
    }

    res.add(call);
  }

  private Graph<ISSABasicBlock> buildDirectedGraph() {
    Graph<ISSABasicBlock> graph = new Graph<>();
    Set<ISSABasicBlock> visited = Sets.newHashSet();
    LinkedList<ISSABasicBlock> worklist = Lists.newLinkedList();
    worklist.add(cfg.entry());
    graph.addEntry(cfg.entry());
    while (!worklist.isEmpty()) {
      ISSABasicBlock curr = worklist.poll();
      if (!visited.add(curr)) {
        continue;
      }
      Iterator<ISSABasicBlock> succNodes = cfg.getSuccNodes(curr);
      while (succNodes.hasNext()) {
        ISSABasicBlock next = succNodes.next();
        graph.addEdge(curr, next);
        worklist.add(next);
      }
    }
    return graph;
  }

  private static class Graph<N> {
    Multimap<N, N> outEdges = HashMultimap.create();
    Multimap<N, N> inEdges = HashMultimap.create();
    Set<N> entries = Sets.newHashSet();

    public void removeNode(N bb) {
      Collection<N> out = outEdges.get(bb);
      Collection<N> in = inEdges.get(bb);

      for (N o : out) {
        for (N i : in) {
          addEdge(i, o);
        }
      }
      if (entries.contains(bb)) {
        entries.remove(bb);
        for (N newEntries : out) {
          entries.add(newEntries);
        }
      }
      for (N tgt : out) {
        inEdges.remove(tgt, bb);
      }
      for (N src : in) {
        outEdges.remove(src, bb);
      }
      outEdges.removeAll(bb);
      inEdges.removeAll(bb);
    }

    public void addEntry(N entry) {
      entries.add(entry);
    }

    private void addEdge(N src, N tgt) {
      outEdges.put(src, tgt);
      inEdges.put(tgt, src);
    }
  }

  public Collection<Statement> getEndPoints() {
    buildCache();
    return endPointCache;
  }

  public Collection<Statement> getSuccsOf(Statement curr) {
    buildCache();
    return succsOfCache.get(curr);
  }

  public Collection<Statement> getPredsOf(Statement curr) {
    buildCache();
    return predsOfCache.get(curr);
  }

  public List<Statement> getStatements() {
    buildCache();
    return statements;
  }

  @Override
  public String toString() {
    String s = "=============================";
    s += "Control Flow Graph for " + method;
    s += "Entries:\n " + Joiner.on("\n").join(startPointCache);
    s += "\nExit:\n " + Joiner.on("\n").join(endPointCache);
    s += "\nSucc:\n ";
    for (Statement e : succsOfCache.keySet()) {
      s += " from " + e + " \n ";
      for (Statement v : succsOfCache.get(e)) {
        s += "\t \t to : " + v + "\n";
      }
    }
    s += "=============================";
    return s;
  }

  public Statement getBranchTarget(int targetIndex) {
    BasicBlock blockForInstruction = cfg.getBlockForInstruction(targetIndex);
    return basicBlockToFirstStmt.get(blockForInstruction);
  }
}
