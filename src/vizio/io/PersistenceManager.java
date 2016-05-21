package vizio.io;

import vizio.Area;
import vizio.Cluster;
import vizio.IDN;
import vizio.Name;
import vizio.Poll;
import vizio.Product;
import vizio.Task;
import vizio.User;
import vizio.Version;

public interface PersistenceManager {

	User user(Name user);

	Product product(Name readName);
	Area area(Name product, Name area);
	Version version(Name product, Name version);
	Task task(Name product, IDN id);
	Task[] tasks(Criteria criteria);

	/**
	 * A user is always stored completely with all its "dependent data".
	 * Besides simple attributes these are a user's sites.
	 *
	 * @param user
	 */
	void persist(User user);

	void persist(Cluster cluster);
	void persist(Product product);
	void persist(Version version);
	void persist(Area area);
	void persist(Poll poll);
	void persist(Task task);

}
