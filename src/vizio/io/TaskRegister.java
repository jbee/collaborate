package vizio.io;

import vizio.Area;
import vizio.Cluster;
import vizio.Poll;
import vizio.Product;
import vizio.Task;
import vizio.Version;

public interface TaskRegister {

	Task[] list(Criteria criteria);

	void persist(Cluster cluster);
	void persist(Product product);
	void persist(Version version);
	void persist(Area area);
	void persist(Poll poll);
	void persist(Task task);
}
