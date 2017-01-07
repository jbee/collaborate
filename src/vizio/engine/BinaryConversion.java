package vizio.engine;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import vizio.engine.DB.Tx;
import vizio.model.Area;
import vizio.model.Date;
import vizio.model.IDN;
import vizio.model.URL;
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
import vizio.model.User;
import vizio.model.Version;

@FunctionalInterface
public interface BinaryConversion<I,O> {

	O convert(I from, ByteBuffer buf);
	
	BinaryConversion<Tx, User> bin2user = (tx,from) -> { 
		User u = new User();
		u.name = bin2name(from);
		u.email = bin2text(from);
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
	
	BinaryConversion<User,ByteBuffer> user2bin = (u,to) -> { 
		name2bin(u.name, to);
		text2bin(u.email, to);
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

	BinaryConversion<Tx, Version> bin2version = (tx,from) -> { 
		Version v = new Version();
		v.product = bin2name(from);
		v.name = bin2name(from);
		v.changeset = bin2names(from);
		return v;
	};

	BinaryConversion<Version,ByteBuffer> version2bin = (v,to) -> { 
		name2bin(v.product, to);
		name2bin(v.name, to);
		names2bin(v.changeset, to);
		return to;
	};
	
	BinaryConversion<Tx, Task> bin2task = (tx,from) -> { 
		Task t = new Task();
		t.product = tx.product(bin2name(from));
		t.area = tx.area(t.product.name, bin2name(from));
		t.id = bin2IDN(from);
		t.serial = bin2IDN(from);
		t.reporter = bin2name(from);
		t.start = bin2date(from);
		t.gist = bin2text(from);
		t.motive = bin2enum(Motive.class, from);
		t.purpose = bin2enum(Purpose.class, from);
		t.status = bin2enum(Status.class, from);
		t.changeset = bin2names(from);
		t.exploitable = from.get() > 0;
		t.cause = bin2IDN(from);
		t.origin = bin2IDN(from);
		t.heat = from.getInt();
		t.base = tx.version(t.product.name, bin2name(from));
		t.enlistedBy = bin2names(from);
		t.approachedBy = bin2names(from);
		t.watchedBy = bin2names(from);
		t.solver = bin2name(from);
		t.end = bin2date(from);
		t.conclusion = bin2text(from);
		t.attachments =  bin2urls(from);
		return t;
	};

	BinaryConversion<Task,ByteBuffer> task2bin = (t,to) -> { 
		name2bin(t.product.name, to);
		name2bin(t.area.name, to);
		IDN2bin(t.id, to);
		IDN2bin(t.serial, to);
		name2bin(t.reporter, to);
		date2bin(t.start, to);
		text2bin(t.gist, to);
		enum2bin(t.motive, to);
		enum2bin(t.purpose, to);
		enum2bin(t.status, to);
		names2bin(t.changeset, to);
		to.put((byte) (t.exploitable ? 1 : 0));
		IDN2bin(t.cause, to);
		IDN2bin(t.origin, to);
		to.putInt(t.heat);
		name2bin(t.base.name, to);
		names2bin(t.enlistedBy, to);
		names2bin(t.approachedBy, to);
		names2bin(t.watchedBy, to);
		name2bin(t.solver, to);
		date2bin(t.end, to);
		text2bin(t.conclusion, to);
		urls2bin(t.attachments, to);
		return to;
	};

	BinaryConversion<Tx, Site> bin2site = (tx,from) -> { 
		Name owner = bin2name(from);
		Name name = bin2name(from);
		String template = bin2text(from);
		return new Site(owner, name, template);
	};

	BinaryConversion<Site,ByteBuffer> site2bin = (site,to) -> { 
		name2bin(site.owner, to);
		name2bin(site.name, to);
		text2bin(site.template, to);
		return to;
	};
	
	BinaryConversion<Tx, Product> bin2product = (tx,from) -> { 
		Product p = new Product();
		p.name = bin2name(from);
		p.tasks = from.getInt();

		p.origin = tx.area(p.name, Name.ORIGIN);
		p.somewhere = tx.area(p.name, Name.UNKNOWN);
		p.somewhen = tx.version(p.name, Name.UNKNOWN);
		return p;
	};

	BinaryConversion<Product,ByteBuffer> product2bin = (p,to) -> { 
		name2bin(p.name, to);
		to.putInt(p.tasks);
		return to;
	};
	
	BinaryConversion<Tx, Poll> bin2poll = (tx,from) -> { 
		Poll p = new Poll();
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

	BinaryConversion<Poll,ByteBuffer> poll2bin = (p,to) -> { 
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
	
	BinaryConversion<Tx, Area> bin2area = (tx,from) -> { 
		Area a = new Area();
		a.product = bin2name(from);
		a.name = bin2name(from);
		a.basis = bin2name(from);
		a.maintainers = bin2names(from);
		a.polls = from.getInt();
		a.tasks = from.getInt();
		a.exclusive = from.get() > 0;
		a.entrance = from.get() > 0;
		a.motive = bin2enum(Motive.class, from);
		a.purpose = bin2enum(Purpose.class, from);
		return a;
	};

	BinaryConversion<Area,ByteBuffer> area2bin = (a,to) -> { 
		name2bin(a.product, to);
		name2bin(a.name, to);
		name2bin(a.basis, to);
		names2bin(a.maintainers, to);
		to.putInt(a.polls);
		to.putInt(a.tasks);
		to.put((byte) (a.exclusive ? 1 : 0));
		to.put((byte) (a.entrance ? 1 : 0));
		enum2bin(a.motive, to);
		enum2bin(a.purpose, to);
		return to;
	};	
	
	/*
	 * Utility helpers
	 */

	static Name bin2name(ByteBuffer from) {
		int len = from.get();
		if (len < 0)
			return null;
		byte[] name = new byte[len];
		from.get(name);
		return Name.fromBytes(name);
	}

	static Names bin2names(ByteBuffer from) {
		int c = from.getShort();
		Name[] names = new Name[c];
		for (int i = 0; i < c; i++) {
			names[i] = bin2name(from);
		}
		return new Names(names);
	}

	static void name2bin(Name name, ByteBuffer to) {
		if (name == null) {
			to.put((byte) -1);
		} else {
			byte[] bytes = name.bytes();
			to.put((byte) bytes.length);
			to.put(bytes);
		}
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

	static void text2bin(String s, ByteBuffer to) {
		if (s == null) {
			to.putInt(-1);
		} else {
			byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
			to.putInt(bytes.length);
			to.put(bytes);
		}
	}	
	
	static String bin2text(ByteBuffer from) {
		int len = from.getInt();
		if (len < 0)
			return null;
		byte[] bytes = new byte[len];
		from.get(bytes);
		return new String(bytes, StandardCharsets.UTF_16);
	}
	
	static URL bin2url(ByteBuffer from) {
		int len = from.getShort();
		if (len < 0)
			return null;
		byte[] bytes = new byte[len];
		from.get(bytes);
		return URL.fromBytes(bytes);
	}
	
	static void url2bin(URL url, ByteBuffer to) {
		to.putShort((short) url.length());
		byte[] bytes = url.bytes();
		to.put((byte) bytes.length);
		to.put(bytes);
	}
	
	static URL[] bin2urls(ByteBuffer from) {
		int c = from.get();
		URL[] attachments = new URL[c];
		for (int i = 0; i < c; i++) {
			attachments[i] = bin2url(from);
		}
		return attachments;
	}
	
	static void urls2bin(URL[] urls, ByteBuffer to) {
		to.put((byte) urls.length);
		for (int i = 0; i < urls.length; i++) {
			url2bin(urls[i], to);
		}
	}
}
