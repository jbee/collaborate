package se.jbee.track.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.model.ByteSequence.find;
import static se.jbee.track.model.Gist.gist;

import org.junit.Test;

public class TestGist {

	@Test
	public void containsGist() {
		Gist a = gist("foo bar baz");

		assertTrue(a.contains(gist("bar")));
		assertTrue(a.contains(gist("baz")));
		assertTrue(a.contains(gist("foo")));
		assertTrue(a.contains(gist("oo ")));
		assertFalse(a.contains(gist("foob")));
		assertFalse(a.contains(gist("bazi")));
	}

	@Test
	public void findGist() {
		Gist a = gist("foo bar baz");

		assertEquals(8, find(a, gist("bar")));
		assertEquals(16, find(a, gist("baz")));
		assertEquals(0, find(a, gist("foo")));
		assertEquals(2, find(a, gist("oo ")));
		assertEquals(-1, find(a, gist("foob")));
		assertEquals(-1, find(a, gist("bazi")));
	}

	@Test
	public void gistLengthIsInCharacters() {
		assertEquals(0, gist("").length());
		assertEquals(1, gist("x").length());
		assertEquals(2, gist("xy").length());
		assertEquals(3, gist("xyz").length());
	}

	@Test
	public void gistCharAtReturnsCharacters() {
		assertEquals('a', gist("a").charAt(0));
		assertEquals('ä', gist("ä").charAt(0));
		assertEquals('€', gist("ä€").charAt(1));
		assertEquals('X', gist("ä€X").charAt(2));
	}
}
