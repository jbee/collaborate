package vizio.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static vizio.engine.BinaryConversion.area2bin;
import static vizio.engine.BinaryConversion.bin2area;
import static vizio.engine.BinaryConversion.bin2poll;
import static vizio.engine.BinaryConversion.bin2product;
import static vizio.engine.BinaryConversion.bin2site;
import static vizio.engine.BinaryConversion.bin2task;
import static vizio.engine.BinaryConversion.bin2user;
import static vizio.engine.BinaryConversion.bin2version;
import static vizio.engine.BinaryConversion.poll2bin;
import static vizio.engine.BinaryConversion.product2bin;
import static vizio.engine.BinaryConversion.site2bin;
import static vizio.engine.BinaryConversion.task2bin;
import static vizio.engine.BinaryConversion.user2bin;
import static vizio.engine.BinaryConversion.version2bin;
import static vizio.model.Name.as;

import java.nio.ByteBuffer;

import org.junit.Test;

import vizio.engine.BinaryConversion;
import vizio.engine.Tracker;
import vizio.engine.Tx;
import vizio.model.Area;
import vizio.model.Entity;
import vizio.model.IDN;
import vizio.model.Name;
import vizio.model.Poll;
import vizio.model.Poll.Matter;
import vizio.model.Product;
import vizio.model.Site;
import vizio.model.Task;
import vizio.model.User;
import vizio.model.Version;

public class TestBinaryConversion {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(TestBinaryConversion.this::tick, (l) -> true);

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void userStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		assertConsistentConversion(bin2user, user2bin, user1);
	}
	
	@Test
	public void siteStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Site site1 = tracker.launch(as("my-tasks"), "foobar", user1);
		assertConsistentConversion(bin2site, site2bin, site1);
	}

	@Test
	public void productStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.constitute(as("p1"), user1);
		assertConsistentConversion(bin2product, product2bin, prod1);
	}

	@Test
	public void areaStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.constitute(as("p1"), user1);
		Area area1 = tracker.compart(prod1, as("area1"), user1);
		assertConsistentConversion(bin2area, area2bin, area1);
	}

	@Test
	public void versionStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.constitute(as("p1"), user1);
		Version v1 = tracker.tag(prod1, as("v1"), user1);
		assertConsistentConversion(bin2version, version2bin, v1);
	}

	@Test
	public void pollStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.constitute(as("p1"), user1);
		User user2 = tracker.register(as("user2"), "user2@example.com", "user2pwd", "salt");
		Poll poll1 = tracker.poll(Matter.inclusion, prod1.origin, user1, user2);
		assertConsistentConversion(bin2poll, poll2bin, poll1);
	}

	@Test
	public void taskStreamer() {
		User user1 = tracker.register(as("user1"), "user1@example.com", "user1pwd", "salt");
		Product prod1 = tracker.constitute(as("p1"), user1);
		user1 = tracker.activate(user1, Tracker.md5("user1pwd"+"salt"));
		Task task1 = tracker.reportDefect(prod1, "broken", user1, prod1.somewhere, prod1.somewhen, true);
		assertConsistentConversion(bin2task, task2bin, task1);
	}

	static <T> void assertConsistentConversion(BinaryConversion<Tx,T> reader, BinaryConversion<T, ByteBuffer> writer, T value) {
		ByteBuffer buf = ByteBuffer.allocate(2048);
		writer.convert(value, buf);
		byte[] written = new byte[buf.position()];
		buf.flip();
		buf.get(written);
		assertTrue(written.length > 0);
		T read = reader.convert(new TestPM(), ByteBuffer.wrap(written));
		buf = ByteBuffer.allocate(2048);
		writer.convert(read, buf);
		byte[] rewritten = new byte[buf.position()];
		buf.flip();
		buf.get(rewritten);
		assertArrayEquals(written, rewritten);
	}

	static class TestPM implements Tx {

		@Override
		public User user(Name user) {
			User res = new User();
			res.name = user;
			return res;
		}
		
		@Override
		public Site site(Name user, Name site) {
			return new Site(user, site, "");
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
		public void put(Entity<?> e) {
			// TODO Auto-generated method stub
			
		}

	}
}
