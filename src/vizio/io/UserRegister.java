package vizio.io;

import vizio.Name;
import vizio.User;

/**
 * There is one user register per installation. Physically each user is stored
 * in a file. Their "transaction boundary" is this user file. A user's data has
 * no "dependencies" to other data that would need to be enforced by a more
 * advanced transaction management.
 *
 * @author jan
 */
public interface UserRegister {

	User fetch(Name name);

	/**
	 * A user is always stored completely with all its "dependent data".
	 * Besides simple attributes these are a user's sites.
	 *
	 * @param user
	 */
	void persist(User user);
}
