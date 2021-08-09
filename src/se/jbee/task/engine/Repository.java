package se.jbee.task.engine;

import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import se.jbee.task.model.Area;
import se.jbee.task.model.ID;
import se.jbee.task.model.IDN;
import se.jbee.task.model.Name;
import se.jbee.task.model.Output;
import se.jbee.task.model.Page;
import se.jbee.task.model.Poll;
import se.jbee.task.model.Task;
import se.jbee.task.model.User;
import se.jbee.task.model.Version;

public interface Repository extends AutoCloseable {

	User user(Name user) throws UnknownEntity;
	Output output(Name output) throws UnknownEntity;
	Area area(Name output, Name area) throws UnknownEntity;
	Version version(Name output, Name version) throws UnknownEntity;
	Task task(Name output, IDN id) throws UnknownEntity;
	Page page(Name output, Name user, Name page) throws UnknownEntity;
	Poll poll(Name output, Name area, IDN serial) throws UnknownEntity;
	Event event(long timestamp) throws UnknownEntity;
	History history(ID entity) throws UnknownEntity;

	void tasks(Name output, Predicate<Task> consumer);
	
	Output[] outputs();
	Page[] pages(Name output, Name menu);
	Poll[] polls(Name output, Name area);
	
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
	
	/*
	 * default methods for convenience
	 */
	
	default Output outputOrNull(Name output) {
		return orNull( () -> output(output));
	}
	
	default User userOrNull(Name user) {
		return orNull( () -> user(user));
	}
	
	default Area areaOrNull(Name output, Name area) {
		return orNull( () -> area(output, area));
	}
	
	default Version versionOrNull(Name output, Name version) {
		return orNull( () -> version(output, version));
	}
	
	static <T> T orNull(Supplier<T> s) {
		try {
			return s.get();
		} catch (UnknownEntity e) {
			return null;
		}
	}

}
