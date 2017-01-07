package vizio.engine;

import java.nio.charset.StandardCharsets;

import vizio.model.Identifier;
import vizio.model.Name;

public final class Limit extends Identifier<Limit> {

	private static final char DIVIDER = ':';
	
	private Limit(byte[] symbols) {
		super(symbols);
	}

	public static Limit limit(String type, Name name) {
		return new Limit((DIVIDER+type+DIVIDER+name).getBytes(StandardCharsets.US_ASCII));
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
