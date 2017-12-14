package vizio;

import static org.junit.Assert.fail;

import java.util.BitSet;

import org.junit.Test;

import vizio.engine.Change;
import vizio.model.Heat;
import vizio.model.Mail;
import vizio.model.Motive;
import vizio.model.Outcome;
import vizio.model.Poll;
import vizio.model.Purpose;
import vizio.model.Status;
import vizio.model.UseCode;
import vizio.model.User;
import vizio.model.Mail.Delivery;
import vizio.model.Mail.Subject;
import vizio.model.Poll.Matter;
import vizio.model.User.Notifications;

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
		assertUniqueCode(User.Notifications.class);
	}
	
	private static <E extends Enum<E>> void assertUniqueCode(Class<E> type) {
		final E[] enumConstants = type.getEnumConstants();
		if (enumConstants.length > 128)
			fail("All enums must not use more than 128 constants!");
		if (!type.isAnnotationPresent(UseCode.class))
			return;
		BitSet set = new BitSet(128);
		for (E c : enumConstants) {
			int idx = c.name().charAt(0);
			if (set.get(idx))
			fail("Code is used twice: "+c.name());
			set.set(idx);
		}
	}
}
