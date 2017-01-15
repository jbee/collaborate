package vizio.ui.ctrl;

import vizio.model.ID;
import vizio.model.IDN;
import vizio.model.Name;

public class Context {

	/**
	 * The current user/viewer/actor, {@link Name#UNKNOWN} for users not logged
	 * in, an email name for anonymous users that did provide their mail during
	 * the session.
	 */
	public Name currentUser;

	public Action action;
	public ID.Type type;

	// SUBSTITUTION DATA (parameters that have been extracted from inputs like the URL)
	public IDN serial;
	public IDN task;
	public Name product;
	public Name version;
	public Name area;
	public Name user;
	public Name site;

	/**
	 * A user or a internal space like <code>@my</code>, <code>@anonymous</code>
	 * or <code>@product</code>.
	 */
	public Name owner() {
		return user;
	}

}
