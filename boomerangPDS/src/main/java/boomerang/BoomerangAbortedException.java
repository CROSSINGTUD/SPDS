package boomerang;

import boomerang.stats.IBoomerangStats;

public class BoomerangAbortedException extends RuntimeException {

    private static final long serialVersionUID = 3767732949845559629L;

    private final IBoomerangStats stats;

    public BoomerangAbortedException(IBoomerangStats stats) {
        this.stats = stats;
    }

    public IBoomerangStats getStats() {
        return stats;
    }

    @Override
    public String toString() {
        return "Boomerang aborted " + getStats();
    }
}
