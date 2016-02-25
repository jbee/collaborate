package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.Date.date;

import org.junit.Test;

public class TestTracker {

	private Tracker tracker = new Tracker(() -> System.currentTimeMillis());

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		Date today = date(currentTimeMillis());
		User user = new User();
		Task task = tracker.track(Motive.defect, Goal.clarification, "A problem", user);

		tracker.lift(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temp(today));
		assertEquals(1, user.liftedToday);
		assertTrue(user.millisLifted >= before);
		assertFalse(user.canLift(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(75, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(87, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(93, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(96, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(98, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(99, task.temp(today));

		user = new User();
		tracker.lift(task, user);
		assertEquals(100, task.temp(today));
	}
}
