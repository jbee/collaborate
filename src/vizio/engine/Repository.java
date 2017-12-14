package vizio.engine;

import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import vizio.model.Area;
import vizio.model.ID;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Poll;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.User;
import vizio.model.Version;

public interface Repository extends AutoCloseable {

	User user(Name user) throws UnknownEntity;
	Product product(Name product) throws UnknownEntity;
	Area area(Name product, Name area) throws UnknownEntity;
	Version version(Name product, Name version) throws UnknownEntity;
	Task task(Name product, IDN id) throws UnknownEntity;
	
	void tasks(Name product, Predicate<Task> consumer);

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
