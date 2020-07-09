package boomerang.scene;

import boomerang.scene.jimple.JimpleDeclaredMethod;
import boomerang.scene.jimple.JimpleMethod;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.queue.QueueReader;

public class SootDataFlowScope {
  private static final Logger LOGGER = LoggerFactory.getLogger(SootDataFlowScope.class);
  private static final String HASH_CODE_SUB_SIG = "int hashCode()";
  private static final String TO_STRING_SUB_SIG = "java.lang.String toString()";
  private static final String EQUALS_SUB_SIG = "boolean equals(java.lang.Object)";
  private static final String CLONE_SIG = "java.lang.Object clone()";
  public static Predicate<SootClass>[] classFilters;
  public static Predicate<SootMethod>[] methodFilters;

  /**
   * Default data-flow scope that only excludes phantom and native methods.
   *
   * @param scene
   * @return
   */
  public static DataFlowScope make(Scene scene) {
    reset();
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        JimpleDeclaredMethod m = (JimpleDeclaredMethod) method;
        return ((SootClass) m.getDeclaringClass().getDelegate()).isPhantom() || m.isNative();
      }

      public boolean isExcluded(Method method) {
        JimpleMethod m = (JimpleMethod) method;
        return ((SootClass) m.getDeclaringClass().getDelegate()).isPhantom() || m.isNative();
      }
    };
  }

  /**
   * Excludes hashCode, toString, equals methods and the implementors of java.util.Collection,
   * java.util.Maps and com.google.common.collect.Multimap
   */
  public static DataFlowScope excludeComplex(Scene scene) {
    reset();
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        JimpleDeclaredMethod m = (JimpleDeclaredMethod) method;
        for (Predicate<SootClass> f : classFilters) {
          if (f.apply((SootClass) m.getDeclaringClass().getDelegate())) {
            return true;
          }
        }
        for (Predicate<SootMethod> f : methodFilters) {
          if (f.apply((SootMethod) m.getDelegate())) {
            return true;
          }
        }
        return ((SootClass) m.getDeclaringClass().getDelegate()).isPhantom() || m.isNative();
      }

      public boolean isExcluded(Method method) {
        JimpleMethod m = (JimpleMethod) method;
        for (Predicate<SootClass> f : classFilters) {
          if (f.apply((SootClass) m.getDeclaringClass().getDelegate())) {
            return true;
          }
        }
        for (Predicate<SootMethod> f : methodFilters) {
          if (f.apply(m.getDelegate())) {
            return true;
          }
        }
        return ((SootClass) m.getDeclaringClass().getDelegate()).isPhantom() || m.isNative();
      }
    };
  }

  private static class MapFilter implements Predicate<SootClass> {
    private static final String MAP = "java.util.Map";
    private static final String GUAVA_MAP = "com.google.common.collect.Multimap";
    private Set<SootClass> excludes = Sets.newHashSet();

    public MapFilter() {
      List<SootClass> mapSubClasses =
          Scene.v().getActiveHierarchy().getImplementersOf(Scene.v().getSootClass(MAP));
      excludes.addAll(mapSubClasses);
      if (Scene.v().containsClass(GUAVA_MAP)) {
        SootClass c = Scene.v().getSootClass(GUAVA_MAP);
        if (c.isInterface()) {
          excludes.addAll(Scene.v().getActiveHierarchy().getImplementersOf(c));
        }
      }
      for (SootClass c : Scene.v().getClasses()) {
        if (c.hasOuterClass() && excludes.contains(c.getOuterClass())) excludes.add(c);
      }
      if (excludes.isEmpty()) {
        LOGGER.warn("Excludes empty for {}", MAP);
      }
    }

    @Override
    public boolean apply(SootClass c) {
      return excludes.contains(c);
    }
  }

  private static class IterableFilter implements Predicate<SootClass> {
    private static final String ITERABLE = "java.lang.Iterable";
    private Set<SootClass> excludes = Sets.newHashSet();

    public IterableFilter() {
      List<SootClass> iterableSubClasses =
          Scene.v().getActiveHierarchy().getImplementersOf(Scene.v().getSootClass(ITERABLE));
      excludes.addAll(iterableSubClasses);
      for (SootClass c : Scene.v().getClasses()) {
        if (c.hasOuterClass() && excludes.contains(c.getOuterClass())) excludes.add(c);
      }
      if (excludes.isEmpty()) {
        LOGGER.warn("Excludes empty for {}", ITERABLE);
      }
    }

    @Override
    public boolean apply(SootClass c) {
      return excludes.contains(c);
    }
  }

  private static class SubSignatureFilter implements Predicate<SootMethod> {
    private Set<SootMethod> excludes = Sets.newHashSet();

    public SubSignatureFilter(String subSig) {
      QueueReader<MethodOrMethodContext> l = Scene.v().getReachableMethods().listener();
      while (l.hasNext()) {
        SootMethod m = l.next().method();
        if (m.getSubSignature().equals(subSig)) {
          excludes.add(m);
        }
      }
      if (excludes.isEmpty()) {
        LOGGER.warn("Excludes empty for {}", subSig);
      }
    }

    @Override
    public boolean apply(SootMethod m) {
      return excludes.contains(m);
    }
  }

  private static void reset() {
    classFilters = new Predicate[] {new MapFilter(), new IterableFilter()};
    methodFilters =
        new Predicate[] {
          new SubSignatureFilter(HASH_CODE_SUB_SIG),
          new SubSignatureFilter(TO_STRING_SUB_SIG),
          new SubSignatureFilter(EQUALS_SUB_SIG),
          new SubSignatureFilter(CLONE_SIG)
        };
  }
}
