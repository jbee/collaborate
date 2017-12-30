package se.jbee.track.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static se.jbee.track.engine.Change.authenticate;
import static se.jbee.track.engine.Change.launch;
import static se.jbee.track.engine.Change.register;
import static se.jbee.track.model.Email.email;
import static se.jbee.track.model.Name.as;
import static se.jbee.track.model.Template.template;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import se.jbee.track.db.DB;
import se.jbee.track.db.DB.TxR;
import se.jbee.track.db.DB.TxRW;
import se.jbee.track.db.LMDB;
import se.jbee.track.model.ID;
import se.jbee.track.model.ID.Type;
import se.jbee.track.model.Name;
import se.jbee.track.model.Site;
import se.jbee.track.model.User;

public class TestLMDB {

	@Rule
	public final TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void putGetLowLevelAPI() throws IOException {
		final File path = tmp.newFolder();
		try (Env<ByteBuffer> env = Env.create()
				.setMapSize(1014*1024*10)
				.setMaxDbs(10)
				.open(path)) {

			Dbi<ByteBuffer> db = env.openDbi(Type.Site.name(), DbiFlags.MDB_CREATE);
			ByteBuffer key = ByteBuffer.allocateDirect(env.getMaxKeySize());
			key.put("foo".getBytes()).flip();
			ByteBuffer val = ByteBuffer.allocateDirect(env.getMaxKeySize());
			val.put("bar".getBytes()).flip();
			db.put(key, val);
			
			try (Txn<ByteBuffer> tx = env.txnRead()) {
				ByteBuffer v = db.get(tx, key);
				Assert.assertNotNull(v);
				byte[] v2 = new byte[v.remaining()];
				v.get(v2);
				assertArrayEquals("bar".getBytes(), v2);
			}
		}
	}
	
	@Test
	public void putGetAdapterAPI() throws IOException {
		Tracker tracker = new Tracker(() -> System.currentTimeMillis(), new NoLimits());
		final File path = tmp.newFolder();
		try (Env<ByteBuffer> env = Env.create()
				.setMapSize(1014*1024*10)
				.setMaxDbs(10)
				.open(path)) {
			DB db = new LMDB(env);
			User u1 = tracker.register(null, as("user1"), email("pass1"));
			u1 = tracker.authenticate(u1, u1.otp);
			Site s1 = tracker.launch(as("def"), template("ghi"), u1);
			Site s2 = tracker.launch(as("mno"), template("pqr"), u1);
			try (TxRW tx = db.write()) {
				ByteBuffer buf = ByteBuffer.allocateDirect(1024);
				Convert.site2bin.convert(s1, buf);
				buf.flip();
				tx.put(s1.uniqueID(), buf);
				buf.clear();
				Convert.site2bin.convert(s2, buf);
				buf.flip();
				tx.put(s2.uniqueID(), buf);
				buf.clear();
				Convert.user2bin.convert(u1, buf);
				buf.flip();
				tx.put(u1.uniqueID(), buf);
				buf.clear();
				tx.commit();
			}
			Site s1r;
			Site s2r;
			User u1r;
			try (TxR tx = db.read()) {
				ByteBuffer buf = tx.get(s1.uniqueID());
				s1r = Convert.bin2site.convert(null, buf);
				buf = tx.get(s2.uniqueID());
				s2r = Convert.bin2site.convert(null, buf);
				buf = tx.get(u1.uniqueID());
				u1r = Convert.bin2user.convert(null, buf);
			}
			assertEquals(s1.name, s1r.name);
			assertEquals(s2.name, s2r.name);
			assertEquals(u1.alias, u1r.alias);
		}
	}
	
	@Test
	public void putGetTranactionAPI() throws IOException {
		final File path = tmp.newFolder();
		try (Env<ByteBuffer> env = Env.create()
				.setMapSize(1014*1024*10)
				.setMaxDbs(10)
				.open(path)) {
			DB db = new LMDB(env);
			Name user = as("abc");
			Name site = as("def");
			Clock realTime = () -> System.currentTimeMillis();
			LinearLimits limits = new LinearLimits(5);

			Change change = register(user, email("test@example.com"));
			Changes changed = Transaction.run(change, db , realTime, limits);
			
			assertEquals(1, changed.length());
			
			assertArrayEquals(new Change.Operation[]{Change.Operation.register}, changed.get(0).transitions);
			assertNull(changed.get(0).before);
			assertNotNull(changed.get(0).after);

			User usr = (User)changed.get(0).after;
			change = authenticate(user, usr.otp).and(launch(user, site, template("ghi")));
			changed = Transaction.run(change, db, realTime, limits);
					
			assertEquals(2, changed.length());
			
			assertArrayEquals(new Change.Operation[]{Change.Operation.launch}, changed.get(1).transitions);
			assertNull(changed.get(1).before);
			assertNotNull(changed.get(1).after);
			assertSame(User.class, changed.get(0).after.getClass());
			assertSame(Site.class, changed.get(1).after.getClass());
			
			Site s;
			User u;
			History sh;
			Event e;
			try (TxR tx = db.read()) {
				ByteBuffer buf = tx.get(ID.siteId(user, site));
				s = Convert.bin2site.convert(null, buf);
				buf = tx.get(ID.userId(user));
				u = Convert.bin2user.convert(null, buf);
				ID hid = ID.historyId(s.uniqueID());
				buf = tx.get(hid);
				sh = Convert.bin2history.convert(hid, buf);
				buf = tx.get(ID.eventId(changed.timestamp));
				e = Convert.bin2event.convert(null, buf);
			}
			assertEquals(site, s.name);
			assertEquals(user, u.alias);
			assertNotNull(sh);
			assertEquals(1, sh.length());
			assertNotNull(e);
			assertEquals(changed.timestamp, e.timestamp);
			assertEquals(ID.userId(user), e.originator);
			assertEquals(2, e.cardinality());
			assertEquals(Change.Operation.authenticate, e.transition(0).ops[0]);
			assertEquals(Change.Operation.launch, e.transition(1).ops[0]);
		}		
	}
}
