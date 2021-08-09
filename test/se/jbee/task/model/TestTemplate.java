package se.jbee.task.model;

import static org.junit.Assert.assertEquals;
import static se.jbee.task.model.Template.parseTemplate;

import org.junit.Test;

public class TestTemplate {

	@Test
	public void templateIsSplitOnBlankLinesAndCriterias() {
		Template t = parseTemplate("***Hello\n\nNobody told me that it is like this!\n[supporter=peter]\n[age>10]\nStange days indeed.\n  \nLast par.");

		assertEquals(5, t.items());
		assertEquals("***Hello\n", t.item(0));
		assertEquals("Nobody told me that it is like this!\n", t.item(1));
		assertEquals(Criteria.class, t.item(2).getClass());
		assertEquals("Stange days indeed.\n", t.item(3));
		assertEquals("Last par.\n", t.item(4));
	}

	@Test
	public void illegalCharactersInTemplatesAreIdentified() {
		try {
			parseTemplate("💩");
		} catch (IllegalArgumentException e) {
			assertEquals("Template contains illegal characters: 💩", e.getMessage());
		}
	}
}
