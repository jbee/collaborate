package vizio.io.stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static vizio.Name.as;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

import vizio.Area;
import vizio.IDN;
import vizio.Name;
import vizio.Poll;
import vizio.Poll.Matter;
import vizio.Product;
import vizio.Task;
import vizio.Tracker;
import vizio.User;
import vizio.Version;
import vizio.io.Criteria;
import vizio.io.EntityManager;
import vizio.io.Streamer;

public class TestStreamer {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(TestStreamer.this::tick, (l,n) -> true);

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void userStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		assertConsistentStream(new UserStreamer(), user1);
	}

	@Test
	public void productStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.initiate(as("p1"), user1);
		assertConsistentStream(new ProductStreamer(), prod1);
	}

	@Test
	public void areaStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.initiate(as("p1"), user1);
		Area area1 = tracker.compart(prod1, as("area1"), user1);
		assertConsistentStream(new AreaStreamer(), area1);
	}

	@Test
	public void versionStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.initiate(as("p1"), user1);
		Version v1 = tracker.tag(prod1, as("v1"), user1);
		assertConsistentStream(new VersionStreamer(), v1);
	}

	@Test
	public void pollStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.initiate(as("p1"), user1);
		User user2 = tracker.register(as("user2"), "user2@example.com", "user2pwd", "salt");
		Poll poll1 = tracker.poll(Matter.inclusion, prod1.origin, user1, user2);
		assertConsistentStream(new PollStreamer(), poll1);
	}

	@Test
	public void taskStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.initiate(as("p1"), user1);
		tracker.activate(user1, Tracker.md5("user1pwd"+"salt"));
		Task task1 = tracker.reportDefect(prod1, "broken", user1, prod1.somewhere, prod1.somewhen, true);
		assertConsistentStream(new TaskStreamer(), task1);
	}

	static <T> void assertConsistentStream(Streamer<T> streamer, T value) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bos);
			streamer.write(value, out);
			byte[] written = bos.toByteArray();
			assertTrue(written.length > 0);
			DataInputStream in = new DataInputStream(new ByteArrayInputStream(written));
			T read = streamer.read(in, new TestPM());
			bos = new ByteArrayOutputStream();
			out = new DataOutputStream(bos);
			streamer.write(read, out);
			byte[] rewritten = bos.toByteArray();
			assertArrayEquals(written, rewritten);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static class TestPM implements EntityManager {

		@Override
		public User user(Name user) {
			User res = new User();
			res.name = user;
			return res;
		}

		@Override
		public Product product(Name product) {
			Product res = new Product();
			res.name = product;
			return res;
		}

		@Override
		public Area area(Name product, Name area) {
			Area res = new Area();
			res.product = product;
			res.name = area;
			return res;
		}

		@Override
		public Version version(Name product, Name version) {
			Version res = new Version();
			res.product = product;
			res.name = version;
			return res;
		}

		@Override
		public Task task(Name product, IDN id) {
			Task res = new Task();
			res.product = product(product);
			res.id = id;
			return res;
		}

		@Override
		public Poll poll(Name product, Name area, IDN serial) {
			Poll res = new Poll();
			res.serial = serial;
			res.area = area(product, area);
			return res;
		}

		@Override
		public Task[] tasks(Criteria criteria) {
			return new Task[0];
		}

		@Override
		public void update(User user) {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Product product) {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Version version) {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Area area) {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Poll poll) {
			// TODO Auto-generated method stub

		}

		@Override
		public void update(Task task) {
			// TODO Auto-generated method stub

		}

	}
}
