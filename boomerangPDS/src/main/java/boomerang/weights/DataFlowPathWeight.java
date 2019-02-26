package boomerang.weights;

import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.jimple.Statement;
import wpds.impl.Weight;

public class DataFlowPathWeight extends Weight {

    private static DataFlowPathWeight one;
    private static DataFlowPathWeight zero;

    /**
     * This set keeps track of all statement that use an alias from source to sink.
     */
    private Set<Statement> allStatements;

    /**
     * A subset of {@link #allStatements} that lists only the last usage of a variable. When data-flow at branches is
     * joined, the set can contain multiple statement that use the variable
     */
    private Set<Statement> lastStatements;

    private String rep;

    private DataFlowPathWeight(String rep) {
        this.rep = rep;
    }

    private DataFlowPathWeight(Set<Statement> allStatement, Set<Statement> lastStatements) {
        this.allStatements = allStatement;
        this.lastStatements = lastStatements;
    }

    public DataFlowPathWeight(Statement relevantStatement) {
        allStatements = Sets.newHashSet();
        lastStatements = Sets.newHashSet();
        allStatements.add(relevantStatement);
        lastStatements.add(relevantStatement);
    }

    @Override
    public Weight extendWith(Weight o) {
        if (!(o instanceof DataFlowPathWeight))
            throw new RuntimeException("Cannot extend to different types of weight!");
        DataFlowPathWeight other = (DataFlowPathWeight) o;
        if (other.equals(one()))
            return this;
        if (this.equals(one()))
            return other;
        if (other.equals(zero()) || this.equals(zero())) {
            return zero();
        }
        Set<Statement> newAllStatements = Sets.newHashSet();
        newAllStatements.addAll(allStatements);
        newAllStatements.addAll(other.allStatements);
        return new DataFlowPathWeight(newAllStatements, other.lastStatements);
    }

    @Override
    public Weight combineWith(Weight other) {
        return extendWith(other);
    }

    public static DataFlowPathWeight one() {
        if (one == null)
            one = new DataFlowPathWeight("ONE");
        return one;
    }

    public static DataFlowPathWeight zero() {
        if (zero == null)
            zero = new DataFlowPathWeight("ZERO");
        return zero;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((allStatements == null) ? 0 : allStatements.hashCode());
        result = prime * result + ((lastStatements == null) ? 0 : lastStatements.hashCode());
        result = prime * result + ((rep == null) ? 0 : rep.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataFlowPathWeight other = (DataFlowPathWeight) obj;
        if (allStatements == null) {
            if (other.allStatements != null)
                return false;
        } else if (!allStatements.equals(other.allStatements))
            return false;
        if (lastStatements == null) {
            if (other.lastStatements != null)
                return false;
        } else if (!lastStatements.equals(other.lastStatements))
            return false;
        if (rep == null) {
            if (other.rep != null)
                return false;
        } else if (!rep.equals(other.rep))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "\nLast relevant: " + lastStatements + "\nAll statements: " + allStatements;
    }
}
