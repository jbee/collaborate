package vizio.ctrl;

import vizio.IDN;
import vizio.Name;

public class Context {

	public Action action;
	public Name product;
	public Name area;
	public Name version;
	public IDN task;
	public Name user;
	public Name site;

	public Name space() {
		return user; // TODO it depends
	}

	public Name site() {
		return site; // TODO it depends
	}

}
