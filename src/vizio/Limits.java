package vizio;

public interface Limits {

	/**
	 * Getting one step closer to the named limit.
	 *
	 * @param limit the limit to use.
	 * @return true in case the limit is not reached so the operation can be done.
	 */
	boolean approach(Name limit, long now);
}
