package vizio.engine;

/**
 * {@link Limits} are basically counters that are increased by
 * {@link #stress(Limit, Clock)}ing them and that decrease by themselves over
 * time. Each counter has a maximum limit. When the maximum is reached the limit
 * or counter cannot be {@link #stress(Limit, Clock)}ed any more.
 * 
 * In principle limits are shared by all users. Users can however
 * {@link #alloc(Limit, Clock)}ate a limit exclusively. Than no other user may
 * {@link #stress(Limit, Clock)} or {@link #alloc(Limit, Clock)} that limit
 * until the limit is {@link #free(Limit)}ed again by the holder, otherwise this
 * causes a {@link ConcurrentUsage}.
 */
public interface Limits {

	/**
	 * Getting one step closer to the maximum of the counter given by limit.
	 *
	 * @param l the counter increased.
	 * @return true in case the limit is not reached so the operation can be done.
	 * @throws ConcurrentUsage in case the limit already was {@link #alloc(Limit, Clock)}ed
	 */
	boolean stress(Limit l, Clock clock) throws ConcurrentUsage;
	
	boolean alloc(Limit l, Clock clock) throws ConcurrentUsage;

	void free(Limit l);
	
	final class ConcurrentUsage extends RuntimeException {

		public ConcurrentUsage(Limit l) {
			super("Limit is already allocated by another transaction: "+l);
		}
		
	}
}
