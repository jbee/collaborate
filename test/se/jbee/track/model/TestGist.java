package se.jbee.track.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.model.Gist.gist;

import org.junit.Test;

public class TestGist {

	@Test
	public void contains() {
		Gist a = gist("foo bar baz");
		
		assertTrue(a.contains(gist("bar")));
		assertTrue(a.contains(gist("baz")));
		assertTrue(a.contains(gist("foo")));
		assertTrue(a.contains(gist("oo ")));
		assertFalse(a.contains(gist("foob")));
		assertFalse(a.contains(gist("bazi")));
	}
}
