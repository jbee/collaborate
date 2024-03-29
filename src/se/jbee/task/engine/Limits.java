package se.jbee.task.engine;

/**
 * {@link Limits} could be short for "Temporary interaction limits".
 *
 * {@link Limits} are basically counters that are increased by
 * {@link #stress(Limit, Clock)}ing them and that decrease by themselves over
 * time. Each counter has a maximum limit. When the maximum is reached the limit
 * or counter cannot be {@link #stress(Limit, Clock)}ed any more.
 *
 * In principle limits are shared by all users. Users can however
 * {@link #block(Limit, Clock)} a limit exclusively. Than no other user may
 * {@link #stress(Limit, Clock)} or {@link #block(Limit, Clock)} that limit
 * until the limit is {@link #unblock(Limit)}ed again by the holder, otherwise this
 * causes a {@link ConcurrentUsage}.
 */
public interface Limits {

	Limits NONE = new NoLimits();

	/**
	 * Getting one step closer to the maximum of the counter given by limit.
	 *
	 * @param l the counter increased.
	 * @return true in case the limit is not reached so the operation can be done.
	 * @throws ConcurrentUsage in case the limit already was {@link #block(Limit, Clock)}ed
	 */
	boolean stress(Limit l, Clock clock) throws ConcurrentUsage;

	boolean block(Limit l, Clock clock) throws ConcurrentUsage;

	void unblock(Limit l) throws ConcurrentUsage;

	final class ConcurrentUsage extends RuntimeException {

		public ConcurrentUsage(Limit l) {
			super("Limit is already blocked by another transaction: " + l);
		}

	}
}
