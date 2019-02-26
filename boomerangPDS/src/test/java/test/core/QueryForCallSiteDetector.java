package test.core;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.callgraph.CalleeListener;
import boomerang.callgraph.ObservableICFG;
import boomerang.callgraph.ObservableStaticICFG;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.seedfactory.SimpleSeedFactory;
import boomerang.util.AccessPath;
import boomerang.util.AccessPathParser;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

import java.util.*;

class QueryForCallSiteDetector extends SimpleSeedFactory {

    boolean resultsMustNotBeEmpty = false;
    boolean accessPathQuery = false;
    boolean integerQueries;
    Set<AccessPath> expectedAccessPaths = new HashSet<>();

    QueryForCallSiteDetector(ObservableICFG<Unit, SootMethod> icfg) {
        super(icfg);
    }

    protected Collection<? extends Query> generate(SootMethod method, Stmt u, Collection<SootMethod> calledMethods) {
        Optional<? extends Query> query = new FirstArgumentOf("queryFor").test(u);

        if (query.isPresent()) {
            return Collections.singleton(query.get());
        }
        query = new FirstArgumentOf("queryForAndNotEmpty").test(u);

        if (query.isPresent()) {
            resultsMustNotBeEmpty = true;
            return Collections.singleton(query.get());
        }
        query = new FirstArgumentOf("intQueryFor").test(u);
        if (query.isPresent()) {
            integerQueries = true;
            return Collections.singleton(query.get());
        }

        query = new FirstArgumentOf("accessPathQueryFor").test(u);
        if (query.isPresent()) {
            accessPathQuery = true;
            getAllExpectedAccessPath(u, method);
            return Collections.singleton(query.get());
        }
        return Collections.emptySet();
    }

    private void getAllExpectedAccessPath(Stmt u, SootMethod m) {
        Value arg = u.getInvokeExpr().getArg(1);
        if (arg instanceof StringConstant) {
            StringConstant stringConstant = (StringConstant) arg;
            String value = stringConstant.value;
            expectedAccessPaths.addAll(AccessPathParser.parseAllFromString(value, m));
        }
    }

    private class FirstArgumentOf implements ValueOfInterestInUnit {

        private String methodNameMatcher;

        public FirstArgumentOf(String methodNameMatcher) {
            this.methodNameMatcher = methodNameMatcher;
        }

        @Override
        public Optional<? extends Query> test(Stmt unit) {
            Stmt stmt = unit;
            if (!(stmt.containsInvokeExpr()))
                return Optional.empty();
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (!invokeExpr.getMethod().getName().matches(methodNameMatcher))
                return Optional.empty();
            Value param = invokeExpr.getArg(0);
            if (!(param instanceof Local))
                return Optional.empty();
            SootMethod newMethod = icfg.getMethodOf(unit);
            Statement newStatement = new Statement(unit, newMethod);
            Val newVal = new Val(param, newMethod);
            BackwardQuery newBackwardQuery = new BackwardQuery(newStatement, newVal);
            return Optional.<Query> of(newBackwardQuery);
        }
    }

}
