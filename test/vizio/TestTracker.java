package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.Date.date;
import static vizio.Name.named;

import org.junit.Test;

public class TestTracker {

	private Tracker tracker = new Tracker(() -> System.currentTimeMillis());

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		long now = currentTimeMillis();
		Date today = date(now);
		Product product = new Product();
		product.name = named("test");
		User user = new User();
		Task task = tracker.reportDefect(product, "A problem", user, null, null, false);

		tracker.support(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temp(today));
		assertEquals(1, user.supportedToday);
		assertTrue(user.millisSupported >= before);
		assertFalse(user.canSupport(currentTimeMillis()));

		user = new User();
		tracker.support(task, user);
		assertEquals(75, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(87, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(93, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(96, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(98, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(99, task.temp(today));

		user = new User();
		tracker.support(task, user);
		assertEquals(100, task.temp(today));
	}
}
