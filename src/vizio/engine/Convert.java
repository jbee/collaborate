package vizio.engine;

import static vizio.model.Attachments.attachments;
import static vizio.model.Gist.gist;

import java.nio.ByteBuffer;

import com.sun.xml.internal.fastinfoset.sax.SystemIdResolver;

import vizio.engine.Change.Tx;
import vizio.model.Area;
import vizio.model.Attachments;
import vizio.model.Bytes;
import vizio.model.Date;
import vizio.model.Email;
import vizio.model.Gist;
import vizio.model.IDN;
import vizio.model.Motive;
import vizio.model.Name;
import vizio.model.Names;
import vizio.model.Outcome;
import vizio.model.Poll;
import vizio.model.Poll.Matter;
import vizio.model.Product;
import vizio.model.Purpose;
import vizio.model.Site;
import vizio.model.Status;
import vizio.model.Task;
import vizio.model.Template;
import vizio.model.URL;
import vizio.model.User;
import vizio.model.Version;

@FunctionalInterface
public interface Convert<I,O> {

	/**
	 * Two way binary conversion.
	 * 
	 * Entity to binary: Entity is input, the passed {@link ByteBuffer} the output
	 * Binary to entity: {@link Tx} and {@link ByteBuffer} are input, a entity the output.
	 * 
	 * @param from an entity (to binary) or a {@link Tx} context (from binary)
	 * @param buf a buffer used for input or output depending on the case.
	 * @return the passed buffer (to binary) or an entity (from binary)
	 */
	O convert(I from, ByteBuffer buf);
	
	Convert<Tx, User> bin2user = (tx,from) -> { 
		User u = new User(from.getInt());
		u.name = bin2name(from);
		u.email = Email.fromBytes(getShortBytes(from));
		byte[] md5 = new byte[from.get()];
		from.get(md5);
		u.md5 = md5;
		u.activated = from.get() > 0;
		u.sites = bin2names(from);
		u.watches = from.getInt();
		u.millisLastActive = from.getLong();
		u.xp = from.getInt();
		u.absolved = from.getInt();
		u.resolved = from.getInt();
		u.dissolved = from.getInt();
		u.millisEmphasised = from.getLong();
		u.emphasisedToday = from.getInt();
		return u;
	};
	
	Convert<User,ByteBuffer> user2bin = (u,to) -> { 
		to.putInt(u.version);
		name2bin(u.name, to);
		putShortBytes(u.email, to);
		to.put((byte) u.md5.length);
		to.put(u.md5);
		to.put((byte) (u.activated ? 1 : 0));
		names2bin(u.sites, to);
		to.putInt(u.watches);
		to.putLong(u.millisLastActive);
		to.putInt(u.xp);
		to.putInt(u.absolved);
		to.putInt(u.resolved);
		to.putInt(u.dissolved);
		to.putLong(u.millisEmphasised);
		to.putInt(u.emphasisedToday);		
		return to; 
	};

	Convert<Tx, Version> bin2version = (tx,from) -> { 
		Version v = new Version(from.getInt());
		v.product = bin2name(from);
		v.name = bin2name(from);
		v.changeset = bin2names(from);
		return v;
	};

	Convert<Version,ByteBuffer> version2bin = (v,to) -> { 
		to.putInt(v.version);
		name2bin(v.product, to);
		name2bin(v.name, to);
		names2bin(v.changeset, to);
		return to;
	};
	
	Convert<Tx, Task> bin2task = (tx,from) -> { 
		Task t = new Task(from.getInt());
		t.product = tx.product(bin2name(from));
		t.area = tx.area(t.product.name, bin2name(from));
		t.id = bin2IDN(from);
		t.serial = bin2IDN(from);
		t.reporter = bin2name(from);
		t.start = bin2date(from);
		t.gist = bin2gist(from);
		t.motive = bin2enum(Motive.class, from);
		t.purpose = bin2enum(Purpose.class, from);
		t.status = bin2enum(Status.class, from);
		t.changeset = bin2names(from);
		t.exploitable = from.get() > 0;
		t.basis = bin2IDN(from);
		t.origin = bin2IDN(from);
		t.emphasis = from.getInt();
		t.base = tx.version(t.product.name, bin2name(from));
		t.pursuedBy = bin2names(from);
		t.engagedBy = bin2names(from);
		t.watchedBy = bin2names(from);
		t.solver = bin2name(from);
		t.end = bin2date(from);
		t.conclusion = bin2gist(from);
		t.attachments =  bin2urls(from);
		return t;
	};

	Convert<Task,ByteBuffer> task2bin = (t,to) -> { 
		to.putInt(t.version);
		name2bin(t.product.name, to);
		name2bin(t.area.name, to);
		IDN2bin(t.id, to);
		IDN2bin(t.serial, to);
		name2bin(t.reporter, to);
		date2bin(t.start, to);
		gist2bin(t.gist, to);
		enum2bin(t.motive, to);
		enum2bin(t.purpose, to);
		enum2bin(t.status, to);
		names2bin(t.changeset, to);
		to.put((byte) (t.exploitable ? 1 : 0));
		IDN2bin(t.basis, to);
		IDN2bin(t.origin, to);
		to.putInt(t.emphasis);
		name2bin(t.base.name, to);
		names2bin(t.pursuedBy, to);
		names2bin(t.engagedBy, to);
		names2bin(t.watchedBy, to);
		name2bin(t.solver, to);
		date2bin(t.end, to);
		gist2bin(t.conclusion, to);
		urls2bin(t.attachments, to);
		return to;
	};

	Convert<Tx, Site> bin2site = (tx,from) -> { 
		int version = from.getInt();
		Name owner = bin2name(from);
		Name name = bin2name(from);
		Template template = Template.fromBytes(getIntBytes(from));
		return new Site(version, owner, name, template);
	};

	Convert<Site,ByteBuffer> site2bin = (site,to) -> { 
		to.putInt(site.version);
		name2bin(site.owner, to);
		name2bin(site.name, to);
		putIntBytes(site.template, to);
		return to;
	};
	
	Convert<Tx, Product> bin2product = (tx,from) -> { 
		Product p = new Product(from.getInt());
		p.name = bin2name(from);
		p.tasks = from.getInt();
		int c = from.get();
		p.integrations = new Product.Integration[c];
		for (int i = 0; i < c; i++) {
			p.integrations[i] = new Product.Integration(bin2name(from), bin2url(from));
		}
		
		// not stored as part of product
		p.origin = tx.area(p.name, Name.ORIGIN);
		p.somewhere = tx.area(p.name, Name.UNKNOWN);
		p.somewhen = tx.version(p.name, Name.UNKNOWN);
		return p;
	};

	Convert<Product,ByteBuffer> product2bin = (p,to) -> { 
		to.putInt(p.version);
		name2bin(p.name, to);
		to.putInt(p.tasks);
		to.put((byte) p.integrations.length);
		for (int i = 0; i < p.integrations.length; i++) {
			name2bin(p.integrations[i].name, to);
			url2bin(p.integrations[i].base, to);
		}
		return to;
	};
	
	Convert<Tx, Poll> bin2poll = (tx,from) -> { 
		Poll p = new Poll(from.getInt());
		p.serial = new IDN(from.getInt());
		p.area = tx.area(bin2name(from), bin2name(from));
		p.matter = bin2enum(Matter.class, from);
		p.affected = tx.user(bin2name(from));
		p.initiator = bin2name(from);
		p.start = bin2date(from);
		p.consenting = bin2names(from);
		p.dissenting = bin2names(from);
		p.expiry = bin2date(from);
		p.end = bin2date(from);
		p.outcome = bin2enum(Outcome.class, from);
		return p;
	};

	Convert<Poll,ByteBuffer> poll2bin = (p,to) -> { 
		to.putInt(p.version);
		IDN2bin(p.serial, to);
		name2bin(p.area.product, to);
		name2bin(p.area.name, to);
		enum2bin(p.matter, to);
		name2bin(p.affected.name, to);
		name2bin(p.initiator, to);
		date2bin(p.start, to);
		names2bin(p.consenting, to);
		names2bin(p.dissenting, to);
		date2bin(p.expiry, to);
		date2bin(p.end, to);
		enum2bin(p.outcome, to);
		return to;
	};
	
	Convert<Tx, Area> bin2area = (tx,from) -> { 
		Area a = new Area(from.getInt());
		a.product = bin2name(from);
		a.name = bin2name(from);
		a.basis = bin2name(from);
		a.maintainers = bin2names(from);
		a.polls = from.getInt();
		a.tasks = from.getInt();
		a.exclusive = from.get() > 0;
		a.board = from.get() > 0;
		a.motive = bin2enum(Motive.class, from);
		a.purpose = bin2enum(Purpose.class, from);
		return a;
	};

	Convert<Area,ByteBuffer> area2bin = (a,to) -> { 
		to.putInt(a.version);
		name2bin(a.product, to);
		name2bin(a.name, to);
		name2bin(a.basis, to);
		names2bin(a.maintainers, to);
		to.putInt(a.polls);
		to.putInt(a.tasks);
		to.put((byte) (a.exclusive ? 1 : 0));
		to.put((byte) (a.board ? 1 : 0));
		enum2bin(a.motive, to);
		enum2bin(a.purpose, to);
		return to;
	};	
	
	/*
	 * Utility helpers
	 */

	static Name bin2name(ByteBuffer from) {
		return Name.fromBytes(getByteBytes(from));
	}

	static Names bin2names(ByteBuffer from) {
		int c = from.getShort();
		Name[] names = new Name[c];
		for (int i = 0; i < c; i++) {
			names[i] = bin2name(from);
		}
		return new Names(names);
	}

	static void name2bin(Name n, ByteBuffer to) {
		putByteBytes(n, to);
	}

	static void names2bin(Names names, ByteBuffer to) {
		to.putShort((short) names.count());
		for (Name name : names) {
			name2bin(name, to);
		}
	}

	static <E extends Enum<E>> void enum2bin(E value, ByteBuffer to) {
		to.putShort((short) (value == null ? -1 : value.ordinal()));
	}

	static <E extends Enum<E>> E bin2enum(Class<E> type, ByteBuffer from) {
		short ordinal = from.getShort();
		return ordinal < 0 ? null : type.getEnumConstants()[ordinal];
	}

	static void date2bin(Date date, ByteBuffer to) {
		to.putInt(date == null ? -1 : date.daysSinceEra);
	}

	static Date bin2date(ByteBuffer from) {
		int daysSinceEra = from.getInt();
		return daysSinceEra < 0 ? null : new Date(daysSinceEra);
	}

	static void IDN2bin(IDN id, ByteBuffer to) {
		to.putInt(id == null ? -1 : id.num);
	}

	static IDN bin2IDN(ByteBuffer from) {
		int num = from.getInt();
		return num < 0 ? null : new IDN(num);
	}

	static void gist2bin(Gist g, ByteBuffer to) {
		putShortBytes(g, to);
	}
	
	static Gist bin2gist(ByteBuffer from) {
		return gist(getShortBytes(from));
	}
	
	static URL bin2url(ByteBuffer from) {
		return URL.fromBytes(getShortBytes(from));
	}
	
	static void url2bin(URL url, ByteBuffer to) {
		putShortBytes(url, to);
	}
	
	static Attachments bin2urls(ByteBuffer from) {
		int c = from.get();
		URL[] attachments = new URL[c];
		for (int i = 0; i < c; i++) {
			attachments[i] = bin2url(from);
		}
		return attachments(attachments);
	}
	
	static void urls2bin(Attachments urls, ByteBuffer to) {
		to.put((byte) urls.length());
		for (URL url : urls) {
			url2bin(url, to);
		}
	}	
	
	static byte[] getByteBytes(ByteBuffer from) {
		return getBytes(from.get(), from);
	}
	
	static byte[] getShortBytes(ByteBuffer from) {
		return getBytes(from.getShort(), from);
	}
	
	static byte[] getIntBytes(ByteBuffer from) {
		return getBytes(from.getInt(), from);
	}
	
	static byte[] getBytes(int len, ByteBuffer from) {
		if (len < 0)
			return null;
		byte[] bytes = new byte[len];
		if (len > 0) {
			from.get(bytes);
		}
		return bytes;
	}
	
	static void putIntBytes(Bytes seq, ByteBuffer to) {
		if (seq == null) {
			to.putInt(-1);
		} else {
			byte[] bytes = seq.bytes();
			to.putInt(bytes.length);
			to.put(bytes);
		}
	}
	
	static void putShortBytes(Bytes seq, ByteBuffer to) {
		if (seq == null) {
			to.putShort((short) -1);
		} else {
			byte[] bytes = seq.bytes();
			to.putShort((short) bytes.length);
			if (bytes.length > 0) {
				to.put(bytes);
			}
		}
	}
	
	static void putByteBytes(Bytes seq, ByteBuffer to) {
		if (seq == null) {
			to.put((byte) -1);
		} else {
			byte[] bytes = seq.bytes();
			to.put((byte) bytes.length);
			to.put(bytes);
		}		
	}
	
}
