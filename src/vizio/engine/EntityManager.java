package vizio.engine;

import vizio.Area;
import vizio.IDN;
import vizio.Name;
import vizio.Poll;
import vizio.Product;
import vizio.Site;
import vizio.Task;
import vizio.User;
import vizio.Version;
import vizio.io.Criteria;

public interface EntityManager {

	User user(Name user);
	Site site(Name user, Name site);
	Poll poll(Name product, Name area, IDN serial);
	Product product(Name product);
	Area area(Name product, Name area);
	Version version(Name product, Name version);
	Task task(Name product, IDN id);

	Task[] tasks(Criteria criteria);

	void update(User user);
	void update(Product product);
	void update(Version version);
	void update(Area area);
	void update(Poll poll);
	void update(Task task);
	void update(Site site);
}
