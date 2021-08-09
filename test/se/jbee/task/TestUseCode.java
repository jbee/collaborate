package se.jbee.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.BitSet;
import java.util.HashMap;

import org.junit.Test;

import se.jbee.task.engine.Change;
import se.jbee.task.model.Cause;
import se.jbee.task.model.Goal;
import se.jbee.task.model.Heat;
import se.jbee.task.model.Mail;
import se.jbee.task.model.Outcome;
import se.jbee.task.model.Status;
import se.jbee.task.model.UseCode;
import se.jbee.task.model.User;
import se.jbee.task.model.Poll.Matter;

/**
 * Make sure the enums that are annotated do follow the "contract" of not having
 * more than one enum constant whose name starts with same letter.
 */
public class TestUseCode {

	@Test
	public void motiveHasUniqueCode() {
		assertUniqueCode(Cause.class);
	}

	@Test
	public void outcomeHasUniqueCode() {
		assertUniqueCode(Outcome.class);
	}

	@Test
	public void purposeHasUniqueCode() {
		assertUniqueCode(Goal.class);
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
	public void objectiveHasUniqueCode() {
		assertUniqueCode(Mail.Objective.class);
	}

	@Test
	public void heatHasUniqueCode() {
		assertUniqueCode(Heat.class);
	}

	@Test
	public void notificationsHaveUniqueCode() {
		assertUniqueCode(Mail.Notification.class);
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
		if (!type.isAnnotationPresent(UseCode.class)) {
			fail("Expected "+type.getSimpleName()+" to be annotated with "+UseCode.class.getSimpleName());
		}
		BitSet set = new BitSet(64);
		for (E c : enumConstants) {
			int idx = c.name().charAt(0);
			if (set.get(idx))
			fail("Code is used twice: "+c.name());
			set.set(idx);
		}
		// additional test: we expect the starting letters to be as annotated. This is
		// just an additional measure to make sure that when we change something the
		// change is valid. This could be one of:
		// 1) adding a new one
		// 2) change order of existing
		// 3) rename while keeping first character same
		char[] expected = type.getAnnotation(UseCode.class).value().toCharArray();
		if (expected.length > enumConstants.length)
			fail("A constant has been removed: Make sure the old code is converted to some default and remove the char from "+UseCode.class.getSimpleName());
		if (expected.length < enumConstants.length)
			fail("A constant has been added: Add its first character to the expected letters in "+UseCode.class);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], enumConstants[i].name().charAt(0));
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
