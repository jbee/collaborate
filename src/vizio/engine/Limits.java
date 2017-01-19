package vizio.engine;

public interface Limits {

	/**
	 * Getting one step closer to the named limit.
	 *
	 * @param l the limit to use.
	 * @return true in case the limit is not reached so the operation can be done.
	 */
	boolean stress(Limit l) throws ConcurrentModification;
	
	interface Assurances extends Limits {

		boolean alloc(Limit l) throws ConcurrentModification;

		void free(Limit l);
		
		Clock clock();
	}
	
	final class ConcurrentModification extends RuntimeException {

		public ConcurrentModification(Limit l) {
			super("Limit is already allocated by another transaction: "+l);
		}
		
	}
}
