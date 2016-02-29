package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.Date.date;
import static vizio.Name.as;

import org.junit.Test;

public class TestTracker {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(TestTracker.this::tick);

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		long now = currentTimeMillis();
		Date today = date(now);
		User user = tracker.register(as("moos"), "moos@example.com", "xxx");
		tracker.activate(user, user.md5);
		Product product = tracker.initiate(as("test"), user);
		Task task = tracker.reportDefect(product, "A problem", user, product.somewhere, product.somewhen, false);

		tracker.stress(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temp(today));
		assertEquals(1, user.stressedToday);
		assertTrue(user.millisStressed >= before);
		assertFalse(user.canStress(currentTimeMillis()));

		user = tracker.register(as("user2"), "user2@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(75, task.temp(today));

		user = tracker.register(as("user3"), "user3@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(87, task.temp(today));

		user = tracker.register(as("user4"), "user4@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(93, task.temp(today));

		user = tracker.register(as("user5"), "user5@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(96, task.temp(today));

		user = tracker.register(as("user6"), "user6@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(98, task.temp(today));

		user = tracker.register(as("user7"), "user8@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(99, task.temp(today));

		user = tracker.register(as("user9"), "user10@example.com", "xxx");
		tracker.activate(user, user.md5);
		tracker.stress(task, user);
		assertEquals(100, task.temp(today));
	}
}
