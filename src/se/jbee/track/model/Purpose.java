package se.jbee.track.model;

@UseCode("cmvp")
public enum Purpose {

	clarification, // confirming defects, localizing area, exploring ideas
	modification,  // something actually changes
	verification,  // something (mostly changes) are checked
	publication    // deployment, acceptance (a change makes it somewhere else)
}
