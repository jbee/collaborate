package se.jbee.task.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * This {@link Limits} will {@link Limits#block(Limit, Clock)} all
 * {@link Limit#isSpecific()} {@link Limits}.
 *
 * This is e.g. useful to detect colliding changes for multiple transaction
 * before we even try to store.
 *
 * Use {@link Limits#unblock(Limit)} with <code>null</code> to free all
 * {@link Limits} occupied so far.
 */
public final class StressBlockSpecificLimits implements Limits {

	private final Set<Limit> blocked = new HashSet<>();
	private final Limits limits;

	public StressBlockSpecificLimits(Limits limits) {
		this.limits = limits;
	}

	private void unblockAllLimits() {
		for (Limit l : blocked) {
			limits.unblock(l);
		}
		blocked.clear();
	}

	@Override
	public boolean stress(Limit limit, Clock clock) throws ConcurrentUsage {
		if (!limit.isSpecific()) {
			return limits.stress(limit, clock);
		}
		return block(limit, clock);
	}

	@Override
	public boolean block(Limit limit, Clock clock) throws ConcurrentUsage {
		if (blocked.contains(limit))
			return true;
		if (limits.block(limit, clock)) {
			blocked.add(limit);
			return true;
		}
		return false;
	}

	@Override
	public void unblock(Limit l) {
		if (l == null) {
			unblockAllLimits();
		} else if (!blocked.contains(l)) {
			throw new ConcurrentUsage(l);
		}
	}
}
