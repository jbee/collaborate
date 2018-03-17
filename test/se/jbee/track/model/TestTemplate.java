package se.jbee.track.model;

import static org.junit.Assert.assertEquals;
import static se.jbee.track.model.Template.template;

import org.junit.Test;

public class TestTemplate {

	@Test
	public void templateIsSplitOnBlankLinesAndCriterias() {
		Template t = template("***Hello\n\nNobody told me that it is like this!\n[user=peter]\n[age>10]\nStange days indeed.\n  \nLast par.");

		Object[] elements = t.elements();

		assertEquals(5, elements.length);
		assertEquals("***Hello\n", elements[0]);
		assertEquals("Nobody told me that it is like this!\n", elements[1]);
		assertEquals(Criteria.class, elements[2].getClass());
		assertEquals("Stange days indeed.\n", elements[3]);
		assertEquals("Last par.\n", elements[4]);
	}

	@Test
	public void illegalCharactersInTemplatesAreIdentified() {
		try {
			template("💩");
		} catch (IllegalArgumentException e) {
			assertEquals("Template contains illegal characters: 💩", e.getMessage());
		}
	}
}
