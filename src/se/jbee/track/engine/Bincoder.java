package se.jbee.track.engine;

import static se.jbee.track.model.Attachments.attachments;
import static se.jbee.track.model.Gist.fromBytes;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map.Entry;

import se.jbee.track.engine.Change.Tx;
import se.jbee.track.model.Area;
import se.jbee.track.model.Attachments;
import se.jbee.track.model.ByteSequence;
import se.jbee.track.model.Date;
import se.jbee.track.model.Email;
import se.jbee.track.model.Gist;
import se.jbee.track.model.ID;
import se.jbee.track.model.IDN;
import se.jbee.track.model.Mail;
import se.jbee.track.model.Motive;
import se.jbee.track.model.Name;
import se.jbee.track.model.Names;
import se.jbee.track.model.Outcome;
import se.jbee.track.model.Output;
import se.jbee.track.model.Page;
import se.jbee.track.model.Poll;
import se.jbee.track.model.Poll.Matter;
import se.jbee.track.model.Purpose;
import se.jbee.track.model.Status;
import se.jbee.track.model.Task;
import se.jbee.track.model.Template;
import se.jbee.track.model.URL;
import se.jbee.track.model.UseCode;
import se.jbee.track.model.User;
import se.jbee.track.model.User.AuthState;
import se.jbee.track.model.Version;

/**
 * A binary de- and encoder (depending on the generic parameters and
 * implementation).
 *
 * @author jan
 *
 * @param <I> additional input type
 * @param <O> output type
 */
@FunctionalInterface
public interface Bincoder<I,O> {

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

	/*
	 * Implementation below...
	 */

	Motive[] motives = Motive.values();
	Outcome[] outcomes = Outcome.values();
	Purpose[] purposes = Purpose.values();
	Status[] status = Status.values();
	Matter[] matters = Matter.values();
	Mail.Delivery[] deliveries = Mail.Delivery.values();
	Change.Operation[] operations = Change.Operation.values();
	Mail.Notification[] notifications = Mail.Notification.values();
	AuthState[] states = AuthState.values();

	/**
	 * Entity Version Numbers (EVN) are used to indicate the format of an
	 * binary entry so that the binary structure can change and older entries
	 * are identified and updated correctly.
	 */
	byte USER_EVN = 1;
	byte TASK_EVN = 1;
	byte POLL_EVN = 1;
	byte AREA_EVN = 1;
	byte VERSION_EVN = 1;
	byte OUTPUT_EVN = 1;
	byte PAGE_EVN = 1;
	byte EVENT_EVN = 1;

	Bincoder<Repository, User> bin2user = (tx,from) -> {
		evn1(from.get()); // just check
		User u = new User(from.getInt());
		u.alias = bin2name(from);
		u.email = Email.fromBytes(getShortBytes(from));
		u.notificationSettings = bin2enumMap(notifications, deliveries, from);
		u.authState = bin2enum(states, from);
		u.authenticated = from.getInt();
		u.encryptedOtp = getShortBytes(from);
		u.millisOtpExprires = from.getLong();
		u.watches = from.getInt();
		u.millisLastActive = from.getLong();
		u.xp = from.getInt();
		u.absolved = from.getInt();
		u.resolved = from.getInt();
		u.dissolved = from.getInt();
		u.abandoned = from.getInt();
		u.millisEmphasised = from.getLong();
		u.emphasisedToday = from.getInt();
		u.contributesToOutputs = bin2names(from);
		return u;
	};

	Bincoder<User,ByteBuffer> user2bin = (u,to) -> {
		to.put(USER_EVN);
		to.putInt(u.version());
		name2bin(u.alias, to);
		putShortBytes(u.email, to);
		enumMap2bin(u.notificationSettings, to);
		enum2bin(u.authState, to);
		to.putInt(u.authenticated);
		putShortBytes(u.encryptedOtp, to);
		to.putLong(u.millisOtpExprires);
		to.putInt(u.watches);
		to.putLong(u.millisLastActive);
		to.putInt(u.xp);
		to.putInt(u.absolved);
		to.putInt(u.resolved);
		to.putInt(u.dissolved);
		to.putInt(u.abandoned);
		to.putLong(u.millisEmphasised);
		to.putInt(u.emphasisedToday);
		names2bin(u.contributesToOutputs, to);
		return to;
	};

	Bincoder<Repository, Version> bin2version = (tx,from) -> {
		evn1(from.get()); // just check
		Version v = new Version(from.getInt());
		v.output = bin2name(from);
		v.name = bin2name(from);
		v.changeset = bin2names(from);
		return v;
	};

	Bincoder<Version,ByteBuffer> version2bin = (v,to) -> {
		to.put(VERSION_EVN);
		to.putInt(v.version());
		name2bin(v.output, to);
		name2bin(v.name, to);
		names2bin(v.changeset, to);
		return to;
	};

	Bincoder<Repository, Task> bin2task = (tx,from) -> {
		evn1(from.get()); // just check
		Task t = new Task(from.getInt());
		t.output = tx.output(bin2name(from));
		t.area = tx.area(t.output.name, bin2name(from));
		t.id = bin2IDN(from);
		t.serial = bin2IDN(from);
		t.reporter = bin2name(from);
		t.reported = bin2date(from);
		t.gist = bin2gist(from);
		t.motive = bin2enum(motives, from);
		t.purpose = bin2enum(purposes, from);
		t.status = bin2enum(status, from);
		t.baseVersions = bin2names(from);
		t.exploitable = from.get() > 0;
		t.disclosed = from.get() > 0;
		t.archived = from.get() > 0;
		t.basis = bin2IDN(from);
		t.origin = bin2IDN(from);
		t.emphasis = from.getInt();
		t.base = tx.version(t.output.name, bin2name(from));
		t.aspirants = bin2names(from);
		t.participants = bin2names(from);
		t.watchers = bin2names(from);
		t.solver = bin2name(from);
		t.resolved = bin2date(from);
		t.conclusion = bin2gist(from);
		t.attachments =  bin2urls(from);
		return t;
	};

	Bincoder<Task,ByteBuffer> task2bin = (t,to) -> {
		to.put(TASK_EVN);
		to.putInt(t.version());
		name2bin(t.output.name, to);
		name2bin(t.area.name, to);
		IDN2bin(t.id, to);
		IDN2bin(t.serial, to);
		name2bin(t.reporter, to);
		date2bin(t.reported, to);
		gist2bin(t.gist, to);
		enum2bin(t.motive, to);
		enum2bin(t.purpose, to);
		enum2bin(t.status, to);
		names2bin(t.baseVersions, to);
		to.put((byte) (t.exploitable ? 1 : 0));
		to.put((byte) (t.disclosed ? 1 : 0));
		to.put((byte) (t.archived ? 1 : 0));
		IDN2bin(t.basis, to);
		IDN2bin(t.origin, to);
		to.putInt(t.emphasis);
		name2bin(t.base.name, to);
		names2bin(t.aspirants, to);
		names2bin(t.participants, to);
		names2bin(t.watchers, to);
		name2bin(t.solver, to);
		date2bin(t.resolved, to);
		gist2bin(t.conclusion, to);
		urls2bin(t.attachments, to);
		return to;
	};

	Bincoder<Repository, Page> bin2page = (tx,from) -> {
		evn1(from.get()); // just check
		int version = from.getInt();
		Name output = bin2name(from);
		Name menu = bin2name(from);
		Name name = bin2name(from);
		Template template = Template.fromBytes(getIntBytes(from));
		return new Page(version, output, menu, name, template);
	};

	Bincoder<Page,ByteBuffer> page2bin = (page,to) -> {
		to.put(PAGE_EVN);
		to.putInt(page.version());
		name2bin(page.output, to);
		name2bin(page.menu, to);
		name2bin(page.name, to);
		putIntBytes(page.template, to);
		return to;
	};

	Bincoder<Repository, Output> bin2output = (tx,from) -> {
		evn1(from.get()); // just check
		Output res = new Output(from.getInt());
		res.name = bin2name(from);
		res.tasks = from.getInt();
		res.categories = bin2names(from);
		int c = from.get();
		res.integrations = new Output.Integration[c];
		for (int i = 0; i < c; i++) {
			res.integrations[i] = new Output.Integration(bin2name(from), bin2url(from));
		}

		// non stored computable fields
		res.origin = tx.area(res.name, Name.ORIGIN);
		res.somewhere = tx.area(res.name, Name.UNKNOWN);
		res.somewhen = tx.version(res.name, Name.UNKNOWN);
		return res;
	};

	Bincoder<Output,ByteBuffer> output2bin = (p,to) -> {
		to.put(OUTPUT_EVN);
		to.putInt(p.version());
		name2bin(p.name, to);
		to.putInt(p.tasks);
		names2bin(p.categories, to);
		to.put((byte) p.integrations.length);
		for (int i = 0; i < p.integrations.length; i++) {
			name2bin(p.integrations[i].name, to);
			url2bin(p.integrations[i].base, to);
		}
		return to;
	};

	Bincoder<Repository, Poll> bin2poll = (tx,from) -> {
		evn1(from.get()); // just check
		Poll p = new Poll(from.getInt());
		p.serial = IDN.idn(from.getInt());
		p.area = tx.area(bin2name(from), bin2name(from));
		p.matter = bin2enum(matters, from);
		p.motivation = bin2gist(from);
		p.affected = bin2name(from);
		p.initiator = bin2name(from);
		p.start = bin2date(from);
		p.consenting = bin2names(from);
		p.dissenting = bin2names(from);
		p.expiry = bin2date(from);
		p.end = bin2date(from);
		p.outcome = bin2enum(outcomes, from);
		return p;
	};

	Bincoder<Poll,ByteBuffer> poll2bin = (p,to) -> {
		to.put(POLL_EVN);
		to.putInt(p.version());
		IDN2bin(p.serial, to);
		name2bin(p.area.output, to);
		name2bin(p.area.name, to);
		enum2bin(p.matter, to);
		gist2bin(p.motivation, to);
		name2bin(p.affected, to);
		name2bin(p.initiator, to);
		date2bin(p.start, to);
		names2bin(p.consenting, to);
		names2bin(p.dissenting, to);
		date2bin(p.expiry, to);
		date2bin(p.end, to);
		enum2bin(p.outcome, to);
		return to;
	};

	Bincoder<Repository, Area> bin2area = (tx,from) -> {
		evn1(from.get()); // just check
		Area a = new Area(from.getInt());
		a.output = bin2name(from);
		a.name = bin2name(from);
		a.basis = bin2name(from);
		a.category = bin2name(from);
		a.maintainers = bin2names(from);
		a.polls = from.getInt();
		a.tasks = from.getInt();
		a.exclusive = from.get() > 0;
		a.abandoned = from.get() > 0;
		a.board = from.get() > 0;
		a.motive = bin2enum(motives, from);
		a.purpose = bin2enum(purposes, from);
		return a;
	};

	Bincoder<Area,ByteBuffer> area2bin = (a,to) -> {
		to.put(AREA_EVN);
		to.putInt(a.version());
		name2bin(a.output, to);
		name2bin(a.name, to);
		name2bin(a.basis, to);
		name2bin(a.category, to);
		names2bin(a.maintainers, to);
		to.putInt(a.polls);
		to.putInt(a.tasks);
		to.put((byte) (a.exclusive ? 1 : 0));
		to.put((byte) (a.abandoned ? 1 : 0));
		to.put((byte) (a.board ? 1 : 0));
		enum2bin(a.motive, to);
		enum2bin(a.purpose, to);
		return to;
	};

	Bincoder<Repository, Event> bin2event = (tx, from) -> {
		evn1(from.get()); // just check
		long timestamp = from.getLong();
		ID user = bin2id(from);
		int n = from.getShort();
		Event.Transition[] changes = new Event.Transition[n];
		for (int i = 0; i < n; i++) {
			ID entity = bin2id(from);
			int sn = from.get();
			Change.Operation[] ops = new Change.Operation[sn];
			for (int j = 0; j < sn; j++) {
				ops[j] = bin2enum(operations, from);
			}
			changes[i] = new Event.Transition(entity, ops);
		}
		return new Event(timestamp, user, changes);
	};

	Bincoder<Event, ByteBuffer> event2bin = (e,to) -> {
		to.put(EVENT_EVN);
		to.putLong(e.timestamp);
		id2bin(e.actor, to);
		to.putShort((short) e.cardinality());
		for (Event.Transition t : e) {
			id2bin(t.entity, to);
			to.put((byte) t.ops.length);
			for (Change.Operation op : t.ops) {
				enum2bin(op, to);
			}
		}
		return to;
	};

	Bincoder<ID, History> bin2history = (id, from) -> {
		long[] events = new long[from.remaining()/Long.BYTES];
		for (int i = 0; i < events.length; i++) {
			events[i] = from.getLong();
		}
		return new History(id, events);
	};

	/*
	 * Utility helpers
	 */

	static ID bin2id(ByteBuffer from) {
		return ID.fromBytes(getByteBytes(from));
	}

	static void id2bin(ID id, ByteBuffer to) {
		putByteBytes(id, to);
	}

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
		if (value == null) {
			to.put((byte)-1);
		} else if (value.getClass().isAnnotationPresent(UseCode.class)) {
			to.put((byte) value.name().charAt(0));
		} else {
			to.put((byte) value.ordinal());
		}
	}

	static <E extends Enum<E>> E bin2enum(E[] constants, ByteBuffer from) {
		byte code = from.get();
		if (code < 0)
			return null;
		// the extra check for range above 65 allows to introduce @UseCode later on
		// ordinal values will most likely be below 64 and codes will definitely be above 64
		// so when we move from ordinal to code we can read both ordinal and code correctly
		// the next store will then change to code
		if (code > 64 && constants[0].getClass().isAnnotationPresent(UseCode.class)) {
			for (E c : constants) {
				if (c.name().charAt(0) == code)
					return c;
			}
			return null;
		}
		return constants[code];
	}

	static <K extends Enum<K>, V extends Enum<V>> void enumMap2bin(EnumMap<K, V> map, ByteBuffer to) {
		if (map == null) {
			to.put((byte) 0);
			return;
		}
		to.put((byte) map.size());
		for (Entry<K, V> e : map.entrySet()) {
			enum2bin(e.getKey(), to);
			enum2bin(e.getValue(), to);
		}
	}

	static <K extends Enum<K>, V extends Enum<V>> EnumMap<K, V> bin2enumMap(K[] keys, V[] values, ByteBuffer from) {
		int l = from.get();
		EnumMap<K, V> res = new EnumMap<>(keys[0].getDeclaringClass());
		for (int i = 0; i < l; i++) {
			res.put(bin2enum(keys, from), bin2enum(values, from));
		}
		return res;
	}

	static void date2bin(Date date, ByteBuffer to) {
		to.putInt(date == null ? -1 : date.epochDay);
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
		return num < 0 ? null : IDN.idn(num);
	}

	static void gist2bin(Gist g, ByteBuffer to) {
		putShortBytes(g, to);
	}

	static Gist bin2gist(ByteBuffer from) {
		return fromBytes(getShortBytes(from));
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
		return getNBytes(from.get(), from);
	}

	static byte[] getShortBytes(ByteBuffer from) {
		return getNBytes(from.getShort(), from);
	}

	static byte[] getIntBytes(ByteBuffer from) {
		return getNBytes(from.getInt(), from);
	}

	static byte[] getNBytes(int n, ByteBuffer from) {
		if (n < 0)
			return null;
		byte[] bytes = new byte[n];
		if (n > 0) {
			from.get(bytes);
		}
		return bytes;
	}

	static void putIntBytes(ByteSequence seq, ByteBuffer to) {
		if (seq == null) {
			to.putInt(-1);
		} else {
			byte[] bytes = seq.readonlyBytes();
			to.putInt(bytes.length);
			to.put(bytes);
		}
	}

	static void putShortBytes(ByteSequence seq, ByteBuffer to) {
		putShortBytes(seq == null ? null : seq.readonlyBytes(), to);
	}

	static void putShortBytes(byte[] bytes, ByteBuffer to) {
		if (bytes == null) {
			to.putShort((short) -1);
		} else {
			to.putShort((short) bytes.length);
			if (bytes.length > 0) {
				to.put(bytes);
			}
		}
	}

	static void putByteBytes(ByteSequence seq, ByteBuffer to) {
		if (seq == null) {
			to.put((byte) -1);
		} else {
			byte[] bytes = seq.readonlyBytes();
			to.put((byte) bytes.length);
			to.put(bytes);
		}
	}

	static void evn1(byte evn) {
	    if (evn != 1) {
	        System.err.println("Unknown object version: "+evn);
	    }
	}

}
