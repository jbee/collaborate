package se.jbee.track;

import static org.junit.Assert.fail;

import java.util.BitSet;
import java.util.HashMap;

import org.junit.Test;

import se.jbee.track.engine.Change;
import se.jbee.track.model.Heat;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Outcome;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Status;
import se.jbee.track.model.UseCode;
import se.jbee.track.model.User;

/**
 * Make sure the enums that are annotated do follow the "contract" of not having
 * more than one enum constant whose name starts with same letter.
 */
public class TestUseCode {

	@Test
	public void motiveHasUniqueCode() {
		assertUniqueCode(Motive.class);
	}
	
	@Test
	public void outcomeHasUniqueCode() {
		assertUniqueCode(Outcome.class);
	}

	@Test
	public void purposeHasUniqueCode() {
		assertUniqueCode(Purpose.class);
	}

	@Test
	public void statusHasUniqueCode() {
		assertUniqueCode(Status.class);
	}

	@Test
	public void matterHasUniqueCode() {
		assertUniqueCode(Matter.class);
	}

	@Test
	public void deliveryHasUniqueCode() {
		assertUniqueCode(Mail.Delivery.class);
	}
	
	@Test
	public void subjectHasUniqueCode() {
		assertUniqueCode(Mail.Subject.class);
	}

	@Test
	public void operationHasUniqueCode() {
		assertUniqueCode(Change.Operation.class);
	}
	
	@Test
	public void heatHasUniqueCode() {
		assertUniqueCode(Heat.class);
	}
	
	@Test
	public void notificationsHaveUniqueCode() {
		assertUniqueCode(User.Notification.class);
	}
	
	@Test
	public void authStatesHaveUniqueCode() {
		assertUniqueCode(User.AuthState.class);
	}
	
	@Test
	public void operationsHaveUniqueHash() {
		assertUniqueHash(Change.Operation.class);
	}
	
	private static <E extends Enum<E>> void assertUniqueCode(Class<E> type) {
		final E[] enumConstants = type.getEnumConstants();
		if (enumConstants.length > 64)
			fail("All enums must not use more than 64 constants!");
		if (!type.isAnnotationPresent(UseCode.class))
			return;
		BitSet set = new BitSet(64);
		for (E c : enumConstants) {
			int idx = c.name().charAt(0);
			if (set.get(idx))
			fail("Code is used twice: "+c.name());
			set.set(idx);
		}
	}
	
	private static <E extends Enum<E>> void assertUniqueHash(Class<E> type) {
		final E[] enumConstants = type.getEnumConstants();
		HashMap<Integer, E> taken = new HashMap<>();
		for (E e : enumConstants) {
			int hash = 0;
			for (char c : e.name().toCharArray()) {
				hash = (hash << 2) + c;
			}
			if (taken.containsKey(hash))
				fail("Equal sum: "+taken.get(hash)+", "+e);
			taken.put(hash, e);
		}
	}
}
