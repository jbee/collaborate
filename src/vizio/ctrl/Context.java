package vizio.ctrl;

import vizio.IDN;
import vizio.Name;

public class Context {

	public Action action;
	public IDN serial;
	public IDN task;
	
	// SUBSTITUTION DATA
	
	public Name product;
	public Name version;
	public Name area;
	
	/**
	 * The current user/viewer/actor, {@link Name#UNKNOWN} for users not logged
	 * in, an email name for anonymous users that did provide their mail during
	 * the session.
	 */
	public Name user;
	
	// VIEW
	
	/**
	 * A user or a internal space like <code>@my</code>, <code>@anonymous</code>
	 * or <code>@product</code>.
	 */
	public Name space;
	
	/**
	 * The name of the site/page within the {@link #space}.
	 */
	public Name site;

}
