package se.jbee.task.api;

public enum Param {

	command, //WHAT: view a user/area page, version, task or do something

	/**
	 * The acting (authenticated) user (this may be set to session ID)
	 */
	actor,
	viewed, // user (whose page do we use)
	role, // as user X (who is @)
	page, // name of the page
	output, // name of the output
	area, // name of the area
	version, // name of the version
	task, // IDN of a task
	serial, // IDN of a board task
	menu, // enum: none, user, area
	category,
	;

	public static enum Command {

		//GETS
		query,   // a page
		examine, // a particular task
		oversee, // a particular version


		// PUTS/POSTS
		enlist, approach, abandon, stress,

		// generates testdata
		sample
	}
}
