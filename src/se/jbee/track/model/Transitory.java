package se.jbee.track.model;

/**
 * Implemented by value types that are stored in a DB but might become
 * {@link #obsolete()} at some point.
 * 
 * If they are stored and {@link #obsolete()} they will be deleted from the DB.
 * 
 * @author jan
 */
public interface Transitory {

	boolean obsolete();
}
