package se.jbee.track.engine;

import se.jbee.track.model.Entity;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Name;

/**
 * All the possible changes wrapped as lazy 'action'.
 * 
 * All {@link Change}s are constructed from keys like {@link Name}s and {@link IDN}s.
 * The actual loading and storing occurs when the {@link Change} is {@link #apply(Tracker, Tx)}ed.
 */
@FunctionalInterface
public interface Change {

	void apply(Tracker t, Tx tx);
	
	default Change and(Change next) {
		return (t, tx) -> { this.apply(t, tx); next.apply(t, tx); };
	}
	
	/**
	 * An application level transaction made available to a {@link Change}. 
	 */
	interface Tx extends Repository {

		void put(Operation op, Entity<?> e);
		
	}
	
	/**
	 * What can be done to tracker data 
	 */
	enum Operation {
		
		/*
		 * !!!OBS!!!
		 * ALWAYS ADD AT THE END (since ordinal is stored) 
		 */
		
		// users
		register,
		confirm,
		authenticate,
		name,
		configure,
		
		// pages
		compose,
		recompose,
		erase,
		
		// outputs
		envision,
		connect,
		disconnect,
		suggest,
		
		// areas
		open, 
		compart,
		leave,
		categorise,
		
		// versions
		tag,
		
		// polls
		poll,
		consent,
		dissent,		
		
		// tasks
		relocate,
		rebase,
		attach,
		detach,
		
		propose,
		indicate,
		warn,
		request,
		remind,
		segment,
		
		absolve,
		resolve,
		dissolve,
		
		archive,
		disclose,
		
		emphasise,

		aspire,
		abandon,
		participate,
		
		watch,
		unwatch;
	}
	
}