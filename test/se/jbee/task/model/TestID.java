package se.jbee.task.model;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.task.model.ID;

public class TestID {

	@Test
	public void base32encoding() {
		assertBase32("0000", 0);
		assertBase32("0001", 1);
		assertBase32("000V", 31);
		assertBase32("0010", 32);
		assertBase32("0020", 64);
		assertBase32("012N", 1111);
		assertBase32("0LME", 22222);
		assertBase32("CU9I", 424242);
		assertBase32("F890", 500000);
		assertBase32("VVVV", 1048575);

	}

	private static void assertBase32(String expected, int actual) {
		assertEquals(expected, new String(ID.toBase32(actual), US_ASCII));
	}
}
