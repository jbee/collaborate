package se.jbee.task.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.task.model.URL.isURL;
import static se.jbee.task.model.URL.url;

import org.junit.Test;

public class TestURL {

	@Test
	public void integratedURLsAreIdentified() {
		assertTrue(url("jira:#34").isIntegrated());
	}

	@Test
	public void unintegratedURLsAreIdentified() {
		assertFalse(url("http://jira:#34").isIntegrated());
		assertFalse(url("/jira:/").isIntegrated());
	}

	@Test
	public void someValidURLs() {
		assertTrue(isURL("http://www.amazon.com/s/ref=nb_sb_noss_1?url=search-alias%3Ddigital-text&amp;field-keywords=Phyllis+Zimbler+Miller"));
	}

}
