package vizio.store;

import vizio.Area;
import vizio.Cluster;
import vizio.Poll;
import vizio.Product;
import vizio.Site;
import vizio.Task;
import vizio.User;
import vizio.Version;

public interface Store {

	Task[] select(Selection selection);

	void persist(Task task);
	void persist(User user);
	void persist(Poll poll);
	void persist(Area area);
	void persist(Site site);
	void persist(Cluster cluster);
	void persist(Product product);
	void persist(Version version);
}
