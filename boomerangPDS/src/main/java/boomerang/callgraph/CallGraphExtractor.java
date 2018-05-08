package boomerang.callgraph;

import soot.*;
import soot.tagkit.Host;
import soot.tagkit.Tag;
import soot.util.Switch;

import java.util.ArrayList;
import java.util.List;

public class CallGraphExtractor implements CalleeListener<Unit, SootMethod>, CallerListener<Unit,SootMethod> {

    static final Unit ALL_UNITS;
    static final SootMethod ALL_METHODS = new SootMethod("ALL", new ArrayList<>(), UnknownType.v());

    //TODO store call graph to the best of your knowledge

    @Override
    public Unit getObservedCaller() {
        return ALL_UNITS;
    }

    @Override
    public SootMethod getObservedCallee() {
        return ALL_METHODS;
    }

    @Override
    public void onCalleeAdded(Unit unit, SootMethod sootMethod) {
        //TODO add edge to call graph
    }

    @Override
    public void onCallerAdded(Unit unit, SootMethod sootMethod) {
        //TODO add edge to call graph
    }

    static {
        ALL_UNITS = new Unit() {
            @Override
            public List<ValueBox> getUseBoxes() {
                return null;
            }

            @Override
            public List<ValueBox> getDefBoxes() {
                return null;
            }

            @Override
            public List<UnitBox> getUnitBoxes() {
                return null;
            }

            @Override
            public List<UnitBox> getBoxesPointingToThis() {
                return null;
            }

            @Override
            public void addBoxPointingToThis(UnitBox unitBox) {

            }

            @Override
            public void removeBoxPointingToThis(UnitBox unitBox) {

            }

            @Override
            public void clearUnitBoxes() {

            }

            @Override
            public List<ValueBox> getUseAndDefBoxes() {
                return null;
            }

            @Override
            public boolean fallsThrough() {
                return false;
            }

            @Override
            public boolean branches() {
                return false;
            }

            @Override
            public void toString(UnitPrinter unitPrinter) {

            }

            @Override
            public void redirectJumpsToThisTo(Unit unit) {

            }

            @Override
            public List<Tag> getTags() {
                return null;
            }

            @Override
            public Tag getTag(String s) {
                return null;
            }

            @Override
            public void addTag(Tag tag) {

            }

            @Override
            public void removeTag(String s) {

            }

            @Override
            public boolean hasTag(String s) {
                return false;
            }

            @Override
            public void removeAllTags() {

            }

            @Override
            public void addAllTagsOf(Host host) {

            }

            @Override
            public int getJavaSourceStartLineNumber() {
                return 0;
            }

            @Override
            public int getJavaSourceStartColumnNumber() {
                return 0;
            }

            @Override
            public void apply(Switch aSwitch) {

            }

            @Override
            public Object clone() {
                return null;
            }
        };
    }
}
