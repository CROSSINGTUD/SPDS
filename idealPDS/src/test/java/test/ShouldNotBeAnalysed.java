package test;

import soot.Unit;


public class ShouldNotBeAnalysed implements Assertion{
    Unit unit;

    ShouldNotBeAnalysed(Unit unit) {
        this.unit = unit;
    }
    public String toString(){
        return "Method should not be included in analysis: " + unit.toString();
    }

    @Override
    public boolean isSatisfied() {
        return false;
    }

    @Override
    public boolean isImprecise() {
        return true;
    }
}
