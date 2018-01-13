package se.jbee.track.engine;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

import se.jbee.track.model.Area;
import se.jbee.track.model.ID;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Name;
import se.jbee.track.model.Output;
import se.jbee.track.model.Page;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Task;
import se.jbee.track.model.User;
import se.jbee.track.model.Version;

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

}
