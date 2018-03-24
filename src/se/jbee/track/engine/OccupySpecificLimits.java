package se.jbee.track.engine;

import java.util.HashSet;
import java.util.Set;

/**
 * This {@link Limits} will {@link Limits#occupy(Limit, Clock)} all
 * {@link Limit#isSpecific()} {@link Limits}.
 *
 * This is e.g. useful to detect colliding changes for multiple transaction
 * before we even try to store.
 *
 * Use {@link Limits#free(Limit)} with <code>null</code> to free all
 * {@link Limits} occupied so far.
 *
 * @author jan
 */
public final class OccupySpecificLimits implements Limits {

	private final Set<Limit> occupied = new HashSet<>();
	private final Limits limits;

	public OccupySpecificLimits(Limits limits) {
		this.limits = limits;
	}

	private void freeOccupiedLimits() {
		for (Limit l : occupied) {
			limits.free(l);
		}
		occupied.clear();
	}

	@Override
	public boolean stress(Limit limit, Clock clock) throws ConcurrentUsage {
		if (!limit.isSpecific()) {
			return limits.stress(limit, clock);
		}
		return occupy(limit, clock);
	}

	@Override
	public boolean occupy(Limit limit, Clock clock) throws ConcurrentUsage {
		if (occupied.contains(limit))
			return true;
		if (limits.occupy(limit, clock)) {
			occupied.add(limit);
			return true;
		}
		return false;
	}

	@Override
	public void free(Limit l) {
		if (l == null) {
			freeOccupiedLimits();
		} else if (!occupied.contains(l)) {
			throw new ConcurrentUsage(l);
		}
	}
}
