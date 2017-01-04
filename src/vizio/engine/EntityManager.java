package vizio.engine;

import vizio.io.Criteria;
import vizio.model.Area;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Poll;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.User;
import vizio.model.Version;

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
