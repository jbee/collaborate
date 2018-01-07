package se.jbee.track.ui.ctrl;

public enum Param {

	command, //WHAT: view a user/area site, version, task or do something
	
	/**
	 * The acting (authenticated) user (this may be set to session ID)
	 */
	actor,
	viewed, // user (whose site do we use)
	role, // as user X (who is @)
	site, // name of the site
	product, // name of the product
	area, // name of the area
	version, // name of the version
	task, // IDN of a task
	serial, // IDN of a board task
	menu, // enum: none, user, area
	// then we use a EnumMap<Param,String> to pass them from e.g. HTTP or a CLI
}
