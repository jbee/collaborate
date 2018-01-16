package se.jbee.track.engine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps track of {@link Limit}s.
 *
 * A limit can be {@link #stress(Limit)}ed or exclusively
 * {@link #occupy(Limit, Clock)}ed. When allocated any further attempt to stress
 * or occupy will cause a {@link ConcurrentUsage} till the occupied
 * {@link Limit} is {@link #free(Limit)}ed again.
 *
 * This way {@link Limits} can act as concurrent modification detection. The
 * caller has to keep track of occupied limits. Double-occupation (even in same
 * thread) is illegal. Free should be called at the end of a modification,
 * successful or not.
 */
public final class LinearLimits implements Limits {

	private static final int ONE_HOUR = 1000*60*60;
	private static final int ONE_DAY = 1000*60*60*24;

	private final int base;
	private final ConcurrentHashMap<Limit, LimitsPerPeriod> stats = new ConcurrentHashMap<>();
	private long nextCleanup;

	public LinearLimits(int base) {
		super();
		this.base = base;
		this.nextCleanup = Long.MIN_VALUE;
	}

	@Override
	public boolean stress(Limit l, Clock clock) {
		return periodLimits(l, clock).stress(clock.time());
	}

	@Override
	public boolean occupy(Limit l, Clock clock) throws ConcurrentUsage {
		LimitsPerPeriod limits = periodLimits(l, clock);
		if (!limits.allocated.compareAndSet(false, true)) {
			throw new ConcurrentUsage(l);
		}
		return limits.stress(clock.time());
	}

	@Override
	public void free(Limit l) {
		if (!periodLimits(l, null).allocated.compareAndSet(true, false)) {
			throw new ConcurrentUsage(l);
		}
	}

	private LimitsPerPeriod periodLimits(Limit l, Clock clock) {
		if (clock != null) {
			long now = clock.time();
			if (now > nextCleanup) {
				cleanup(now);
				nextCleanup = now+ONE_HOUR;
			}
		}
		return stats.computeIfAbsent(l, (li) -> { return new LimitsPerPeriod(li, base); } );
	}

	private void cleanup(long now) {
		Iterator<LimitsPerPeriod> limits = stats.values().iterator();
		while (limits.hasNext()) {
			if (now - limits.next().lastStressed > ONE_DAY) {
				limits.remove();
			}
		}
	}

	private static final class LimitsPerPeriod {

		private final Limit limit;
		private final LimitPerPeriod second;
		private final LimitPerPeriod minute;
		private final LimitPerPeriod quater;
		private final LimitPerPeriod hour;
		private final LimitPerPeriod day;
		final AtomicBoolean allocated = new AtomicBoolean(false);
		long lastStressed;

		LimitsPerPeriod(Limit l, int base) {
			this.limit = l;
			int f = l.factor() * base;
			this.second = new LimitPerPeriod( 1*f, 1000);
			this.minute = new LimitPerPeriod(10*f, 1000*60);
			this.quater = new LimitPerPeriod(20*f, 1000*60*15);
			this.hour   = new LimitPerPeriod(30*f, ONE_HOUR);
			this.day    = new LimitPerPeriod(50*f, ONE_DAY);
		}

		boolean stress(long now) {
			lastStressed=now;
			return second.stress(now) && minute.stress(now) && quater.stress(now) && hour.stress(now) && day.stress(now);
		}

		@SuppressWarnings("boxing")
		@Override
		public String toString() {
			return String.format("%s [s%d/%d m%d/%d q%d/%d h%d/%d d%d/%d]",
					limit, second.count.get(), second.limit,
					minute.count.get(), minute.limit, quater.count.get(),
					quater.limit, hour.count.get(), hour.limit,
					day.count.get(), hour.limit);
		}
	}

	private static final class LimitPerPeriod {

		/**
		 * The absolute limit that cannot be exceeded.
		 */
		final int limit;
		final int periodDivisor;

		long previousPeriod;
		AtomicInteger count = new AtomicInteger();

		LimitPerPeriod(int limit, int periodDivisor) {
			super();
			this.limit = limit;
			this.periodDivisor = periodDivisor;
		}

		boolean stress(long now) {
			long period = now / periodDivisor;
			if (period > previousPeriod) {
				count.set(0);
				previousPeriod = period;
			}
			if (count.get() >= limit) // already larger?
				return false;
			return count.incrementAndGet() < limit; // could we inc it without overflow?
		}

	}
}
