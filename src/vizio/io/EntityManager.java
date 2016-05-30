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

public interface EntityManager {

	User user(Name user);
	Poll poll(Name product, Name area, IDN serial);
	Product product(Name product);
	Area area(Name product, Name area);
	Version version(Name product, Name version);
	Task task(Name product, IDN id);

	Task[] tasks(Criteria criteria);

	void update(User user);
	void update(Cluster cluster);
	void update(Product product);
	void update(Version version);
	void update(Area area);
	void update(Poll poll);
	void update(Task task);

}
