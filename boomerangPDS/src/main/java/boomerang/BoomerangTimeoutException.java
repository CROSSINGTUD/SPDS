package boomerang;

import boomerang.stats.AdvancedBoomerangStats;
import boomerang.stats.IBoomerangStats;

public class BoomerangTimeoutException extends RuntimeException {

	private IBoomerangStats stats;
	private long elapsed;

	public BoomerangTimeoutException(long elapsed, IBoomerangStats stats) {
		this.elapsed = elapsed;
		this.stats = stats;
	}

	@Override
	public String toString() {
		return "Boomerang Timeout after " + elapsed + "ms\n " + stats;
	}
}
