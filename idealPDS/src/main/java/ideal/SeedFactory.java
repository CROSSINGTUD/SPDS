package ideal;

import boomerang.WeightedForwardQuery;
import boomerang.jimple.Statement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import sync.pds.solver.nodes.GeneratedState;
import sync.pds.solver.nodes.INode;
import sync.pds.solver.nodes.SingleNode;
import wpds.impl.*;
import wpds.interfaces.WPAUpdateListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by johannesspath on 07.12.17.
 */
public abstract class SeedFactory<W extends Weight> {

    private final WeightedPushdownSystem<Statement, INode<Reachable>, Weight.NoWeight> pds = new WeightedPushdownSystem<>();
    private final Multimap<WeightedForwardQuery<W>,Transition<Statement, INode<Reachable>>> seedToTransition = HashMultimap.create();
    private final IDEALAnalysisDefinition<W> analysisDefinition;

    private final WeightedPAutomaton<Statement,  INode<Reachable>, Weight.NoWeight> automaton = new WeightedPAutomaton<Statement,  INode<Reachable>, Weight.NoWeight>(wrap(Reachable.v())) {
        @Override
        public INode<Reachable> createState(INode<Reachable> reachable, Statement loc) {
            return new GeneratedState<>(reachable,loc);
        }

        @Override
        public boolean isGeneratedState(INode<Reachable> reachable) {
            return reachable instanceof GeneratedState;
        }


        @Override
        public Statement epsilon() {
            return Statement.epsilon();
        }

        @Override
        public Weight.NoWeight getZero() {
            return Weight.NO_WEIGHT_ZERO;
        }

        @Override
        public Weight.NoWeight getOne() {
            return Weight.NO_WEIGHT_ONE;
        }
    };

    public SeedFactory(IDEALAnalysisDefinition<W> analysisDefinition){
        this.analysisDefinition = analysisDefinition;
    }

    public Set<WeightedForwardQuery<W>> computeSeeds(){
        List<SootMethod> entryPoints = Scene.v().getEntryPoints();
        for(SootMethod m : entryPoints){
            for(Unit u : icfg().getStartPointsOf(m)) {
                addNormalRule(new Statement((Stmt) u, m),new Statement((Stmt) u, m));
            }
        }
        pds.poststar(automaton);
        automaton.registerListener(new WPAUpdateListener<Statement, INode<Reachable>, Weight.NoWeight>() {
            @Override
            public void onWeightAdded(Transition<Statement, INode<Reachable>> t, Weight.NoWeight noWeight, WeightedPAutomaton<Statement, INode<Reachable>, Weight.NoWeight> aut) {
                process(t.getLabel());

                Stmt u = t.getLabel().getUnit().get();
                Collection<SootMethod> calledMethods = (icfg().isCallStmt(u) ? icfg().getCalleesOfCallAt(u)
                        : new HashSet<SootMethod>());
                for(WeightedForwardQuery<W> seed : analysisDefinition.generate(t.getLabel().getMethod(), u, calledMethods)){
                    seedToTransition.put(seed, t);
                }
            }
        });
        return seedToTransition.keySet();
    }

    private void process(Statement curr) {
        Unit currUnit = curr.getUnit().get();
        for(Unit u : icfg().getSuccsOf(currUnit)){
            addNormalRule(curr, new Statement((Stmt) u, curr.getMethod()));
        }
        if(icfg().isCallStmt(currUnit)){
            for(Unit returnSite : icfg().getSuccsOf(currUnit)){
                for(SootMethod callee : icfg().getCalleesOfCallAt(currUnit)){
                    for(Unit sp : icfg().getStartPointsOf(callee)){
                        addPushRule(curr, new Statement((Stmt) sp, callee),new Statement((Stmt)returnSite, curr.getMethod()));
                    }
                }
            }
        }
    }

    private void addPushRule(Statement curr, Statement calleeSp, Statement returnSite) {
        pds.addRule(new PushRule<>(wrap(Reachable.v()),curr,wrap(Reachable.v()),calleeSp,returnSite, Weight.NO_WEIGHT_ONE));
    }

    private void addNormalRule(Statement curr, Statement succ) {
        pds.addRule(new NormalRule<>(wrap(Reachable.v()),curr,wrap(Reachable.v()),succ, Weight.NO_WEIGHT_ONE));
    }

    private INode<Reachable> wrap(Reachable r){
        return new SingleNode<>(r);
    }

    public abstract BiDiInterproceduralCFG<Unit,SootMethod> icfg();

}
