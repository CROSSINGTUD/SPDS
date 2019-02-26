package boomerang.jimple;

import soot.SootMethod;
import soot.Value;

public class ValWithFalseVariable extends Val {
    private final Value falseVariable;

    public ValWithFalseVariable(Value v, SootMethod m, Value instanceofValue) {
        super(v, m);
        this.falseVariable = instanceofValue;
    }

    public Value getFalseVariable() {
        return falseVariable;
    }

    @Override
    public String toString() {
        return "Instanceof " + falseVariable + " " + super.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((falseVariable == null) ? 0 : falseVariable.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValWithFalseVariable other = (ValWithFalseVariable) obj;
        if (falseVariable == null) {
            if (other.falseVariable != null)
                return false;
        } else if (!falseVariable.equals(other.falseVariable))
            return false;
        return true;
    }
}
