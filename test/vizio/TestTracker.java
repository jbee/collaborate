package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.Tracker.lift
;

import org.junit.Test;

public class TestTracker {

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		User user = new User();
		Task task = new Task();

		lift(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temp());
		assertEquals(1, user.liftedToday);
		assertTrue(user.millisLifted >= before);
		assertFalse(user.canLift());

		user = new User();
		lift(task, user);
		assertEquals(75, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(87, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(93, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(96, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(98, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(99, task.temp());

		user = new User();
		lift(task, user);
		assertEquals(100, task.temp());
	}
}
