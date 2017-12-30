package se.jbee.track.engine;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

import se.jbee.track.model.Area;
import se.jbee.track.model.ID;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Name;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Product;
import se.jbee.track.model.Site;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;

public interface Repository extends AutoCloseable {

	User user(Name user) throws UnknownEntity;
	Product product(Name product) throws UnknownEntity;
	Area area(Name product, Name area) throws UnknownEntity;
	Version version(Name product, Name version) throws UnknownEntity;
	Task task(Name product, IDN id) throws UnknownEntity;
	
	void tasks(Name product, Predicate<Task> consumer);
	void products(Predicate<Product> consumer);

	Site site(Name user, Name site) throws UnknownEntity;
	Poll poll(Name product, Name area, IDN serial) throws UnknownEntity;
	Event event(long timestamp) throws UnknownEntity;
	History history(ID entity) throws UnknownEntity;
	
	/**
	 * No {@link Exception} here!
	 */
	@Override
	public void close();
	
	class UnknownEntity extends NoSuchElementException {
		
		public UnknownEntity(ID id) {
			super(id.toString());
		}
		
	}

}
