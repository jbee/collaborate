package vizio.engine;

public interface Limits {

	/**
	 * Getting one step closer to the named limit.
	 *
	 * @param limit the limit to use.
	 * @return true in case the limit is not reached so the operation can be done.
	 */
	boolean stress(Limit limit) throws ConcurrentModification;
	
	final class ConcurrentModification extends RuntimeException {

		public ConcurrentModification(Limit l) {
			super("Limit is already allocated by another transaction: "+l);
		}
		
	}
}
