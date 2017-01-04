package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.Date.date;
import static vizio.Name.as;

import org.junit.Test;

import vizio.engine.Tracker;

public class TestTracker {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(TestTracker.this::tick, (l) -> true);

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		long now = currentTimeMillis();
		Date today = date(now);
		User user = tracker.register(as("moos"), "moos@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		Product product = tracker.found(as("test"), user);
		Task task = tracker.reportDefect(product, "A problem", user, product.somewhere, product.somewhen, false);

		task = tracker.emphasise(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.heatNumeric(today));
		assertEquals(1, user.emphasisedToday);
		assertTrue(user.millisEmphasised >= before);
		assertFalse(user.canEmphasise(currentTimeMillis()));

		user = tracker.register(as("user2"), "user2@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(75, task.heatNumeric(today));

		user = tracker.register(as("user3"), "user3@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(87, task.heatNumeric(today));

		user = tracker.register(as("user4"), "user4@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(93, task.heatNumeric(today));

		user = tracker.register(as("user5"), "user5@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(96, task.heatNumeric(today));

		user = tracker.register(as("user6"), "user6@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(98, task.heatNumeric(today));

		user = tracker.register(as("user7"), "user8@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(99, task.heatNumeric(today));

		user = tracker.register(as("user9"), "user10@example.com", "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(100, task.heatNumeric(today));
	}
}
