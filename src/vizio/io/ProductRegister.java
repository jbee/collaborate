package vizio.io;

import vizio.Area;
import vizio.Cluster;
import vizio.Poll;
import vizio.Product;
import vizio.Task;
import vizio.Version;

public interface ProductRegister {

	Task[] list(Criteria criteria);

	void persist(Task task);
	void persist(Poll poll);
	void persist(Area area);
	void persist(Cluster cluster);
	void persist(Product product);
	void persist(Version version);
}
