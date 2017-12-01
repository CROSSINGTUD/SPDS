package boomerang;

import boomerang.stats.BoomerangStats;

public class BoomerangTimeoutException extends RuntimeException {

	private BoomerangStats stats;
	private long elapsed;

	public BoomerangTimeoutException(long elapsed, BoomerangStats stats) {
		this.elapsed = elapsed;
		this.stats = stats;
	}

	@Override
	public String toString() {
		return "Boomerang Timeout after " + elapsed + "ms\n " + stats;
	}
}
