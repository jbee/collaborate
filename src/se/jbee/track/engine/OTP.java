package se.jbee.track.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A generator for one time passwords.
 * 
 * The generator will initialize on first use. Thereby it is "unknown" what seed
 * will be used. Each password token is derived from a random <tt>long</tt> that
 * is than translated to an alphanumeric "string" of 10 characters given as
 * <tt>byte[]</tt>.
 * 
 * As letters and digits only add up to 62 characters the <tt>$</tt> and
 * <tt>_</tt> symbols were added to possible characters set.
 * 
 * So 6 bits of a long are mapped to one character 10 times using 60 bits of the
 * random long.
 * 
 * When stored the token is encrypted using MD5. This is just an additional step
 * to secure the token integrity. So having access to the DB does not allow to
 * read a users plain token. This will only exist in memory when created and send
 * as email as well as when confirmed and compared again.
 */
public final class OTP {

	private static final AtomicReference<SecureRandom> RND = new AtomicReference<>();

	/**
	 * 64 different ASCII letters, digits and symbols.
	 * 
	 * Each byte is broken down to 4 of these.
	 */
	private static final byte[] MAPPING = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ$_".getBytes(StandardCharsets.US_ASCII);
	
	private static final int BIT_1_TO_6 = 1+2+4+8+16+32;
	
	/**
	 * @return the next random password token. A token is 10 bytes long each
	 *         byte being an ASCII code so that all bytes form an alphanumeric
	 *         token (special symbols $ and _ are possible too).
	 */
	public static byte[] next() {
		SecureRandom rnd = RND.updateAndGet((r) -> r != null ? r : new SecureRandom());
		long val = rnd.nextLong();
		byte[] res = new byte[10];
		for (int i = 0; i < res.length; i++) {
			int v = (int) val;
			val >>= 6;
			res[i] = MAPPING[v & BIT_1_TO_6];
		}
		return res;
	}
	
	public static byte[] encrypt(byte[] token) {
		return md5(token);
	}
	
	public static boolean isToken(byte[] token, byte[] encryptedToken) {
		return Arrays.equals(md5(token), encryptedToken);
	}
	
	private static byte[] md5(byte[] val) {
		try {
			return MessageDigest.getInstance("MD5").digest(val);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
