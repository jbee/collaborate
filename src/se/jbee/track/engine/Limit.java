package se.jbee.track.engine;

import se.jbee.track.model.Identifier;
import se.jbee.track.model.Name;

/**
 * A limit is an {@link Identifier} for a counter. 
 */
public final class Limit extends Identifier<Limit> {

	private static final byte[] DIVIDER = {'#'};
	
	private Limit(byte[] symbols) {
		super(symbols);
	}

	public static Limit limit(String type, Name name) {
		return new Limit(join(DIVIDER, asciiBytes(type), DIVIDER, name.bytes()));
	}
	
	public int factor() {
		char last = charAt(length()-1);
		if (last == '*')
			return 10;
		if (last == '~')
			return 5;
		return 1;
	}
	
	public boolean isSpecific() {
		return factor() == 1;
	}

}
