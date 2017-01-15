package vizio.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static vizio.model.Name.as;

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

import vizio.db.DB.TxR;
import vizio.db.DB.TxW;
import vizio.engine.Convert;
import vizio.engine.Change;
import vizio.engine.LimitControl;
import vizio.engine.Transaction;
import vizio.model.Entity;
import vizio.model.ID;
import vizio.model.ID.Type;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Site;
import vizio.model.User;

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
		final File path = tmp.newFolder();
		try (Env<ByteBuffer> env = Env.create()
				.setMapSize(1014*1024*10)
				.setMaxDbs(10)
				.open(path)) {
			DB db = new LMDB(env);
			Site s1 = new Site(as("abc"), as("def"), "ghi");
			Site s2 = new Site(as("jkl"), as("mno"), "pqr");
			User u1 = new User();
			u1.name = as("user1");
			u1.email = "pass1";
			u1.sites = Names.empty();
			u1.md5 = "foo".getBytes();
			try (TxW tx = db.write()) {
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
			assertEquals(u1.name, u1r.name);
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
			Name name = as("def");
			Change change = 
					Change.register(user, "test@example.com", "foo", "salt").and(
					Change.launch(name, "ghi", user));
					
			Entity<?>[] changed = Transaction.run(change, new LimitControl(() -> System.currentTimeMillis(), 5) ,db);
			
			assertEquals(2, changed.length);
			assertSame(User.class, changed[0].getClass());
			assertSame(Site.class, changed[1].getClass());
			
			Site s;
			User u;
			try (TxR tx = db.read()) {
				ByteBuffer buf = tx.get(ID.siteId(user, name));
				s = Convert.bin2site.convert(null, buf);
				buf = tx.get(ID.userId(user));
				u = Convert.bin2user.convert(null, buf);
			}
			assertEquals(name, s.name);
			assertEquals(user, u.name);
		}		
	}
}
