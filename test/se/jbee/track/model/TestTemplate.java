package se.jbee.track.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestTemplate {

	@Test
	public void templateIsSplitOnBlankLinesAndCriterias() {
		Template t = Template.template("***Hello\n\nNobody told me that it is like this!\n[user=peter]\n[age>10]\nStange days indeed.\n  \nLast par.");
		
		Object[] elements = t.elements();
		
		assertEquals(5, elements.length);
		assertEquals("***Hello\n", elements[0]);
		assertEquals("Nobody told me that it is like this!\n", elements[1]);
		assertEquals(Criteria.class, elements[2].getClass());
		assertEquals("Stange days indeed.\n", elements[3]);
		assertEquals("Last par.\n", elements[4]);
	}
}
