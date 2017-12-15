package se.jbee.track.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import se.jbee.track.engine.OTP;

public class TestOTP {

	@Test
	public void tokensAre10CharactersLong() {
		assertEquals(10, OTP.next().length);
		assertEquals(10, OTP.next().length);
		assertEquals(10, OTP.next().length);
	}
	
	@Test
	public void tokensAreAlphanumeric() {
		assertTrue(new String(OTP.next()).matches("[a-zA-Z0-9$_]{10}"));
		assertTrue(new String(OTP.next()).matches("[a-zA-Z0-9$_]{10}"));
		assertTrue(new String(OTP.next()).matches("[a-zA-Z0-9$_]{10}"));
	}
	
	@Test
	public void tokensChange() {
		byte[][] tries = new byte[10][];
		for (int i = 0; i < tries.length; i++) {
			tries[i] = OTP.next();
			for (int j = 0; j < i; j++) {
				assertArraysNotEqual(tries[i], tries[j]);
			}
		}
	}
	
	private static void assertArraysNotEqual(byte[] a, byte[] b) {
		assertEquals(a.length, b.length);
		for (int i = 0; i < a.length; i++)
			if (a[i] != b[i])
				return;
		fail("Arrays are equal: "+Arrays.toString(a));
	}
}
