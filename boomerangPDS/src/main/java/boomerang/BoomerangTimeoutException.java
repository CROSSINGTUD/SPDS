/*******************************************************************************
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Johannes Spaeth - initial API and implementation
 *******************************************************************************/
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
