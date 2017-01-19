package vizio.engine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.engine.Limits.Assurances;

/**
 * Keeps track of {@link Limit}s.
 * 
 * A limit can be {@link #stress(Limit)}ed or exclusively {@link #alloc(Limit)}ated.
 * When allocated any futher attempt to stress or allocate will cause a {@link IllegalStateException}
 * until the allocated {@link Limit} is {@link #free(Limit)}ed again. 
 * 
 * This way {@link Limits} can act as concurrent modification detection.
 * The caller has to keep track of allocated limits. 
 * Reallocation (even in same thread) is  illegal. 
 * Free should be called at the end of a modification, successful or not.
 *  
 * @author jan
 */
public final class LimitControl implements Assurances {
	
	private static final int ONE_HOUR = 1000*60*60;
	private static final int ONE_DAY = 1000*60*60*24;
	
	public final Clock clock;
	private final int base;
	private final ConcurrentHashMap<Limit, LimitStats> stats = new ConcurrentHashMap<>();
	private long nextCleanup;
	
	public LimitControl(Clock clock, int base) {
		super();
		this.clock = clock;
		this.base = base;
		this.nextCleanup = clock.time()+ONE_HOUR;
	}

	@Override
	public Clock clock() {
		return clock;
	}
	
	@Override
	public boolean stress(Limit l) {
		return stats(l).stress(clock.time());
	}

	@Override
	public boolean alloc(Limit l) throws ConcurrentModification {
		LimitStats ls = stats(l);
		if (!ls.allocated.compareAndSet(false, true)) {
			throw new ConcurrentModification(l);
		}
		return ls.stress(clock.time());
	}
	
	@Override
	public void free(Limit l) {
		if (!stats(l).allocated.compareAndSet(true, false)) {
			System.err.println("Tried to free non allocated limit: "+l);
		}
	}

	private LimitStats stats(Limit l) {
		long now = clock.time();
		if (now > nextCleanup) {
			cleanup(now);
			nextCleanup = now+ONE_HOUR;
		}
		return stats.computeIfAbsent(l, (li) -> { return new LimitStats(li, base); } );
	}

	private void cleanup(long now) {
		Iterator<LimitStats> limits = stats.values().iterator();
		while (limits.hasNext()) {
			if (now - limits.next().lastStressed > ONE_DAY) {
				limits.remove();
			}
		}
	}

	private static final class LimitStats {

		private final Limit limit;
		private final PeriodicLimit second;
		private final PeriodicLimit minute;
		private final PeriodicLimit quater;
		private final PeriodicLimit hour;
		private final PeriodicLimit day;
		final AtomicBoolean allocated = new AtomicBoolean(false);
		long lastStressed;
		
		LimitStats(Limit l, int base) { 
			this.limit = l;
			int f = l.factor() * base;
			this.second = new PeriodicLimit( 1*f, 1000);
			this.minute = new PeriodicLimit(10*f, 1000*60);
			this.quater = new PeriodicLimit(20*f, 1000*60*15);
			this.hour   = new PeriodicLimit(30*f, ONE_HOUR);
			this.day    = new PeriodicLimit(50*f, ONE_DAY);
		}		
		
		boolean stress(long now) {
			lastStressed=now;
			return second.stress(now) && minute.stress(now) && quater.stress(now) && hour.stress(now) && day.stress(now);
		}
		
		@Override
		public String toString() {
			return String.format("%s [s%d/%d m%d/%d q%d/%d h%d/%d d%d/%d]", limit, 
					second.count.get(), second.limit, minute.count.get(), minute.limit, quater.count.get(), quater.limit, hour.count.get(), hour.limit, day.count.get(), hour.limit);
		}
	}
	
	private static final class PeriodicLimit {
		
		/**
		 * The absolute limit that cannot be exceeded.
		 */
		final int limit;
		final int periodDivisor;
		
		long previousPeriod;
		AtomicInteger count = new AtomicInteger();
		
		PeriodicLimit(int limit, int periodDivisor) {
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
