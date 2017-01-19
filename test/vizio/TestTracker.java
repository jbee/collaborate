package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.model.Date.date;
import static vizio.model.Email.email;
import static vizio.model.Gist.gist;
import static vizio.model.Name.as;

import org.junit.Test;

import vizio.engine.Tracker;
import vizio.model.Date;
import vizio.model.Product;
import vizio.model.Task;
import vizio.model.User;

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
		User user = tracker.register(as("moos"), email("moos@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		Product product = tracker.constitute(as("test"), user);
		Task task = tracker.reportDefect(product, gist("A problem"), user, product.somewhere, product.somewhen, false);

		task = tracker.emphasise(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temperature(today));
		assertEquals(1, user.emphasisedToday);
		assertTrue(user.millisEmphasised >= before);
		assertFalse(user.canEmphasise(currentTimeMillis()));

		user = tracker.register(as("user2"), email("user2@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(75, task.temperature(today));

		user = tracker.register(as("user3"), email("user3@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(87, task.temperature(today));

		user = tracker.register(as("user4"), email("user4@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(93, task.temperature(today));

		user = tracker.register(as("user5"), email("user5@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(96, task.temperature(today));

		user = tracker.register(as("user6"), email("user6@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(98, task.temperature(today));

		user = tracker.register(as("user7"), email("user8@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(99, task.temperature(today));

		user = tracker.register(as("user9"), email("user10@example.com"), "xxx", "salt");
		user = tracker.activate(user, user.md5);
		task = tracker.emphasise(task, user);
		assertEquals(100, task.temperature(today));
	}
}
