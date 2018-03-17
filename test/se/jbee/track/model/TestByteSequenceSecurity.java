package se.jbee.track.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.track.model.Gist.isGistText;
import static se.jbee.track.model.Template.isTemplateText;

import org.junit.Test;

public class TestByteSequenceSecurity {

	@Test
	public void htmlCannotBeIncludedInGistText() {
		for (char c = 'a'; c <= 'z'; c++)
			assertFalse(isGistText("<"+c));
		for (char c = 'A'; c <= 'Z'; c++)
			assertFalse(isGistText("<"+c));
		assertFalse(isGistText("</"));
		assertFalse(isGistText("/>"));
	}

	@Test
	public void htmlCannotBeIncludedInTemplateText() {
		for (char c = 'a'; c <= 'z'; c++)
			assertFalse(isTemplateText("<"+c));
		for (char c = 'A'; c <= 'Z'; c++)
			assertFalse(isTemplateText("<"+c));
		assertFalse(isTemplateText("</"));
		assertFalse(isTemplateText("/>"));
	}

	@Test
	public void angleBracketsCanBeIncludedInGistText() {
		assertTrue(isGistText("1<2"));
		assertTrue(isGistText("a < b"));
		assertTrue(isGistText("äöüあ"));
		assertTrue(isGistText("age<80"));
	}

	@Test
	public void angleBracketsCanBeIncludedInTemplateText() {
		assertTrue(isTemplateText("1<2"));
		assertTrue(isTemplateText("a < b"));
		assertTrue(isTemplateText("[age<80]"));
		assertTrue(isTemplateText("[output=@]"));
		assertTrue(isTemplateText("äöüあ"));
	}

	@Test
	public void nonLettersOrDigitsUnicodeSymbolsAreNotGistText() {
		assertFalse(isGistText("💩"));
	}

	@Test
	public void currencySymbolsAreGistText() {
		assertTrue(isGistText("€$£¥"));
	}

	@Test
	public void nonLettersOrDigitsUnicodeSymbolsAreNotTemplateText() {
		assertFalse(isTemplateText("💩"));
	}

	@Test
	public void currencySymbolsAreTemplateText() {
		assertTrue(isTemplateText("€$£¥"));
	}
}
