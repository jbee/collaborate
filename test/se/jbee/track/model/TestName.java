package se.jbee.track.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static se.jbee.track.model.Name.as;

import org.junit.Test;

public class TestName {

	@Test
	public void validEditableNames() {
		assertLegalName("x1");
		assertLegalName("X1");
	}
	
	@Test
	public void namesMustStartWithALetter() {
		assertIllegalName("_");
		assertIllegalName("-");
	}
	
	@Test
	public void namesMustHaveAtLeastTwoCharacters() {
		assertIllegalName("A");
		assertIllegalName("a");
		assertIllegalName("_");
		assertIllegalName("-");
	}
	
	@Test
	public void digitOnlyNamesMayHaveOnlyOneCharacter() {
		assertLegalName("1");
		assertLegalName("9");
		assertLegalName("11");
	}
	
	
	private static void assertLegalName(String name) {
		assertNotNull(as(name));
	}
	
	private static void assertIllegalName(String name) {
		try {
			as(name);
			fail("expected illegal name, but was legal: "+name);
		} catch (IllegalArgumentException e) {
			// we expected this
		}
	}
}
