package se.jbee.track.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.model.Bytes.isBasicText;
import static se.jbee.track.model.Bytes.isText;

import org.junit.Test;

public class TestBytes {

	@Test
	public void htmlCannotBeIncludedInBasicText() {
		for (char c = 'a'; c <= 'z'; c++)
			assertFalse(isBasicText("<"+c));
		for (char c = 'A'; c <= 'Z'; c++)
			assertFalse(isBasicText("<"+c));
		assertFalse(isBasicText("</"));
		assertFalse(isBasicText("/>"));
	}
	
	@Test
	public void htmlCannotBeIncludedInText() {
		for (char c = 'a'; c <= 'z'; c++)
			assertFalse(isText("<"+c));
		for (char c = 'A'; c <= 'Z'; c++)
			assertFalse(isText("<"+c));
		assertFalse(isText("</"));
		assertFalse(isText("/>"));
	}
	
	@Test
	public void angleBracketsCanBeIncludedInBasicText() {
		assertTrue(isBasicText("1<2"));
		assertTrue(isBasicText("a < b"));
		assertTrue(isBasicText("äöüあ"));
		assertTrue(isBasicText("age<80"));
	}
	
	@Test
	public void angleBracketsCanBeIncludedInText() {
		assertTrue(isText("1<2"));
		assertTrue(isText("a < b"));
		assertTrue(isText("[age<80]"));
		assertTrue(isText("[output=@]"));
		assertTrue(isText("äöüあ"));
	}
	
	@Test
	public void nonLettersOrDigitsUnicodeSymbolsAreNotBasicText() {
		assertFalse(isBasicText("💩"));
	}
	
	@Test
	public void nonLettersOrDigitsUnicodeSymbolsAreNotText() {
		assertFalse(isText("💩"));
	}
}
