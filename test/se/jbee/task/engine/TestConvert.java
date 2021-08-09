package se.jbee.task.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static se.jbee.task.engine.Bincoder.area2bin;
import static se.jbee.task.engine.Bincoder.bin2area;
import static se.jbee.task.engine.Bincoder.bin2event;
import static se.jbee.task.engine.Bincoder.bin2output;
import static se.jbee.task.engine.Bincoder.bin2page;
import static se.jbee.task.engine.Bincoder.bin2poll;
import static se.jbee.task.engine.Bincoder.bin2task;
import static se.jbee.task.engine.Bincoder.bin2user;
import static se.jbee.task.engine.Bincoder.bin2version;
import static se.jbee.task.engine.Bincoder.event2bin;
import static se.jbee.task.engine.Bincoder.output2bin;
import static se.jbee.task.engine.Bincoder.page2bin;
import static se.jbee.task.engine.Bincoder.poll2bin;
import static se.jbee.task.engine.Bincoder.task2bin;
import static se.jbee.task.engine.Bincoder.user2bin;
import static se.jbee.task.engine.Bincoder.version2bin;
import static se.jbee.task.engine.Server.Switch.OPEN;
import static se.jbee.task.model.Email.email;
import static se.jbee.task.model.Gist.gist;
import static se.jbee.task.model.Name.as;
import static se.jbee.task.model.Template.parseTemplate;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.junit.Test;

import se.jbee.task.engine.Bincoder;
import se.jbee.task.engine.Change;
import se.jbee.task.engine.Event;
import se.jbee.task.engine.History;
import se.jbee.task.engine.NoLimits;
import se.jbee.task.engine.Repository;
import se.jbee.task.engine.Server;
import se.jbee.task.engine.Tracker;
import se.jbee.task.engine.Event.Transition;
import se.jbee.task.model.Area;
import se.jbee.task.model.Email;
import se.jbee.task.model.Gist;
import se.jbee.task.model.ID;
import se.jbee.task.model.IDN;
import se.jbee.task.model.Name;
import se.jbee.task.model.Output;
import se.jbee.task.model.Page;
import se.jbee.task.model.Poll;
import se.jbee.task.model.Task;
import se.jbee.task.model.Template;
import se.jbee.task.model.User;
import se.jbee.task.model.Version;
import se.jbee.task.model.Poll.Matter;

public class TestConvert {

	private long now = System.currentTimeMillis();
	private Tracker tracker = new Tracker(new Server().with(Email.email("admin@example.com")).with(TestConvert.this::tick).with(new NoLimits()).with(OPEN));

	private long tick() {
		now += 60000;
		return now;
	}

	@Test
	public void userConversion() {
		User user1 = newTestUser();
		assertConsistentConversion(bin2user, user2bin, user1);
	}

	@Test
	public void pageConversion() {
		User user1 = newTestUser();
		Page page1 = tracker.compose(user1, as("my-tasks"), parseTemplate("foobar"));
		assertConsistentConversion(bin2page, page2bin, page1);
	}

	@Test
	public void outputConversion() {
		User user1 = newTestUser();
		Output prod1 = tracker.envision(as("p1"), user1);
		assertConsistentConversion(bin2output, output2bin, prod1);
	}

	@Test
	public void areaConversion() {
		User user1 = newTestUser();
		Output prod1 = tracker.envision(as("p1"), user1);
		Area area1 = tracker.compart(prod1, as("area1"), user1);
		assertConsistentConversion(bin2area, area2bin, area1);
	}

	@Test
	public void versionConversion() {
		User user1 = newTestUser();
		Output prod1 = tracker.envision(as("p1"), user1);
		Version v1 = tracker.tag(prod1, as("v1"), user1);
		assertConsistentConversion(bin2version, version2bin, v1);
	}

	@Test
	public void pollConversion() {
		User user1 = newTestUser();
		Output prod1 = tracker.envision(as("p1"), user1);
		User user2 = tracker.register(null, as("user2"), email("user2@example.com"));
		Poll poll1 = tracker.poll(Matter.inclusion, Gist.gist("foo"), prod1.origin, user1, user2);
		assertConsistentConversion(bin2poll, poll2bin, poll1);
	}

	@Test
	public void taskConversion() {
		User user1 = newTestUser();
		Output prod1 = tracker.envision(as("p1"), user1);
		Task task1 = tracker.reportDefect(prod1, gist("broken"), user1, prod1.somewhere, prod1.somewhen, true);
		assertConsistentConversion(bin2task, task2bin, task1);
	}

	@Test
	public void logEntryConversion() {
		long timestamp = System.currentTimeMillis();
		Name user = as("testuser");
		Transition c1 = new Transition(ID.userId(user), Change.Operation.abandon, Change.Operation.attach);
		Transition[] entityChanges = new Transition[] { c1, c1 };
		assertConsistentConversion(bin2event, event2bin, new Event(timestamp, ID.userId(user), entityChanges));
	}

	private User newTestUser() {
		User u1 = tracker.register(null, as("user1"), email("user1@example.com"));
		u1 = tracker.authenticate(u1, u1.otp);
		return u1;
	}

	static <T> void assertConsistentConversion(Bincoder<Repository,T> reader, Bincoder<T, ByteBuffer> writer, T value) {
		ByteBuffer buf = ByteBuffer.allocate(2048);
		writer.convert(value, buf);
		byte[] written = new byte[buf.position()];
		buf.flip();
		buf.get(written);
		assertTrue(written.length > 0);
		T read = reader.convert(new TestRepository(), ByteBuffer.wrap(written));
		buf = ByteBuffer.allocate(2048);
		writer.convert(read, buf);
		byte[] rewritten = new byte[buf.position()];
		buf.flip();
		buf.get(rewritten);
		assertArrayEquals(written, rewritten);
	}

	static class TestRepository implements Repository {

		@Override
		public User user(Name user) {
			User res = new User(1);
			res.alias = user;
			return res;
		}

		@Override
		public Page page(Name output, Name user, Name page) {
			return new Page(1, output, user, page, Template.BLANK_PAGE);
		}

		@Override
		public Output output(Name output) {
			Output res = new Output(1);
			res.name = output;
			return res;
		}

		@Override
		public Area area(Name output, Name area) {
			Area res = new Area(1);
			res.output = output;
			res.name = area;
			return res;
		}

		@Override
		public Version version(Name output, Name version) {
			Version res = new Version(1);
			res.output = output;
			res.name = version;
			return res;
		}

		@Override
		public Task task(Name output, IDN id) {
			Task res = new Task(1);
			res.output = output(output);
			res.id = id;
			return res;
		}

		@Override
		public Poll poll(Name output, Name area, IDN serial) {
			Poll res = new Poll(1);
			res.serial = serial;
			res.area = area(output, area);
			return res;
		}

		@Override
		public Event event(long timestamp) throws UnknownEntity {
			throw new UnknownEntity(ID.eventId(timestamp));
		}

		@Override
		public History history(ID entity) throws UnknownEntity {
			throw new UnknownEntity(ID.historyId(entity));
		}

		@Override
		public void tasks(Name output, Predicate<Task> consumer) {
			// TODO Auto-generated method stub

		}

		@Override
		public Output[] outputs() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Page[] pages(Name output, Name menu) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Poll[] polls(Name output, Name area) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close() {
			// nothing to do
		}

	}
}
