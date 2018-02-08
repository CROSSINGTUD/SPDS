package test;

import soot.Unit;


public class ShouldNotBeAnalyzed implements Assertion{
    private Unit unit;
    private boolean isSatisfied = true;
    private boolean isImprecise = false;

    ShouldNotBeAnalyzed(Unit unit) {
        this.unit = unit;
    }

    //TODO Melanie: Get calling method, atm this is always shouldNotBeAnalyzed
    public String toString(){
        return "Method should not be included in analysis: " + unit.toString();
    }

    @Override
    public boolean isSatisfied() {
        return isSatisfied;
    }

    @Override
    public boolean isImprecise() {
        return isImprecise;
    }

    public void hasBeenAnalyzed(){
        isSatisfied = false;
        isImprecise = true;
    }
}
