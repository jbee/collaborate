package se.jbee.task;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.task.engine.Server.Switch.OPEN;
import static se.jbee.task.model.Date.date;
import static se.jbee.task.model.Email.email;
import static se.jbee.task.model.Gist.gist;
import static se.jbee.task.model.Name.as;

import org.junit.Test;

import se.jbee.task.engine.Limits;
import se.jbee.task.engine.Server;
import se.jbee.task.engine.Tracker;
import se.jbee.task.model.Date;
import se.jbee.task.model.Email;
import se.jbee.task.model.Output;
import se.jbee.task.model.Task;
import se.jbee.task.model.User;

public class TestTracker {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(new Server().with(Email.email("admin@example.com")).with(TestTracker.this::tick).with(Limits.NONE).with(OPEN));

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		long now = currentTimeMillis();
		Date today = date(now);
		User user = tracker.register(null, as("moos"), email("moos@example.com"));
		user = tracker.authenticate(user, user.otp);
		Output output = tracker.envision(as("test"), user);
		Task task = tracker.reportDefect(output, gist("A problem"), user, output.somewhere, output.somewhen, false);

		task = tracker.emphasise(task, user);

		long before = currentTimeMillis();
		assertEquals(50, task.temperature(today));
		assertEquals(1, user.emphasisedToday);
		assertTrue(user.millisEmphasised >= before);
		assertFalse(user.canEmphasise(currentTimeMillis()));

		user = tracker.register(null, as("user2"), email("user2@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(75, task.temperature(today));

		user = tracker.register(null, as("user3"), email("user3@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(87, task.temperature(today));

		user = tracker.register(null, as("user4"), email("user4@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(93, task.temperature(today));

		user = tracker.register(null, as("user5"), email("user5@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(96, task.temperature(today));

		user = tracker.register(null, as("user6"), email("user6@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(98, task.temperature(today));

		user = tracker.register(null, as("user7"), email("user8@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(99, task.temperature(today));

		user = tracker.register(null, as("user9"), email("user10@example.com"));
		user = tracker.authenticate(user, user.otp);
		task = tracker.emphasise(task, user);
		assertEquals(100, task.temperature(today));
	}
}
