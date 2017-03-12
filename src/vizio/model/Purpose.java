package vizio.model;

@UseCode
public enum Purpose {

	clarification, // confirming defects, localizing area, exploring ideas
	modification,  // something actually changes
	verification,  // something is checked
	publication    // deployment, acceptance (a change makes it somewhere else)
}
