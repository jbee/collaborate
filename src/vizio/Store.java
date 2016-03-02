package vizio;

public interface Store {

	Task[] select(Query query);

	void persist(Task task);
	void persist(User user);
	void persist(Poll poll);
	void persist(Area area);
	void persist(Site site);
	void persist(Cluster cluster);
	void persist(Product product);
	void persist(Version version);
}
