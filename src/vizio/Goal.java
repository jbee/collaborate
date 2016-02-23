package vizio;

public enum Goal {

	investigation, // confirming defects, localizing area, exploring ideas
	modification, // something actually changes
	validation, // something is checked
	distribution // deployment, acceptance (a change makes it somewhere else)
}
