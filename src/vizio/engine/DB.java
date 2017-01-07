package vizio.engine;

import java.nio.ByteBuffer;

import vizio.model.Area;
import vizio.model.Entity;
import vizio.model.ID;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Poll;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.User;
import vizio.model.Version;

/**
 * A simple key/value DB abstraction. 
 */
public interface DB {
	
	ByteBuffer get(ID key);
	
	void put(ID key, ByteBuffer value);
	
	/**
	 * A app level transaction.
	 */
	interface Tx {

		User user(Name user);
		Site site(Name user, Name site);
		Poll poll(Name product, Name area, IDN serial);
		Product product(Name product);
		Area area(Name product, Name area);
		Version version(Name product, Name version);
		Task task(Name product, IDN id);

		void put(Entity<?> e);
	}
}