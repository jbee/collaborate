package vizio;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static vizio.VIZIO.vote;

import org.junit.Test;

public class TestVIZIO {

	@Test
	public void heatAddsHalfOfWhatIsMissing() {
		User user = new User();
		Task task = new Task();

		vote(user, task);

		long before = currentTimeMillis();
		assertEquals(50, task.temp());
		assertEquals(1, user.votesToday);
		assertTrue(user.millisVoted >= before);
		assertFalse(user.canVote());

		user = new User();
		vote(user, task);
		assertEquals(75, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(87, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(93, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(96, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(98, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(99, task.temp());

		user = new User();
		vote(user, task);
		assertEquals(100, task.temp());
	}
}
