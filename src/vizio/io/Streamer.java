package vizio.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import vizio.Date;
import vizio.IDN;
import vizio.Name;
import vizio.Names;
import vizio.engine.EntityManager;

public interface Streamer<T> {

	Charset UTF8 = Charset.forName("UTF-8");

	T read(DataInputStream in, EntityManager em) throws IOException;

	void write(T data, DataOutputStream out) throws IOException;

	/*
	 * Utility helpers
	 */

	static Name readName(DataInputStream in) throws IOException {
		int len = in.readByte();
		if (len < 0)
			return null;
		byte[] name = new byte[len];
		in.read(name);
		return Name.fromBytes(name);
	}

	static Names readNames(DataInputStream in) throws IOException {
		int c = in.readUnsignedShort();
		Name[] names = new Name[c];
		for (int i = 0; i < c; i++) {
			names[i] = readName(in);
		}
		return new Names(names);
	}

	static void writeName(Name name, DataOutputStream out) throws IOException {
		if (name == null) {
			out.writeByte(-1);
		} else {
			byte[] bytes = name.bytes();
			out.writeByte(bytes.length);
			out.write(bytes);
		}
	}

	static void writeNames(Names names, DataOutputStream out) throws IOException {
		out.writeShort(names.count());
		for (Name name : names) {
			writeName(name, out);
		}
	}

	static <E extends Enum<E>> void writeEnum(E value, DataOutputStream out) throws IOException {
		out.writeShort(value == null ? -1 : value.ordinal());
	}

	static <E extends Enum<E>> E readEnum(Class<E> type, DataInputStream in) throws IOException {
		short ordinal = in.readShort();
		return ordinal < 0 ? null : type.getEnumConstants()[ordinal];
	}

	static void writeDate(Date date, DataOutputStream out) throws IOException {
		out.writeInt(date == null ? -1 : date.daysSinceEra);
	}

	static Date readDate(DataInputStream in) throws IOException {
		int daysSinceEra = in.readInt();
		return daysSinceEra < 0 ? null : new Date(daysSinceEra);
	}

	static void writeIDN(IDN id, DataOutputStream out) throws IOException {
		out.writeInt(id == null ? -1 : id.num);
	}

	static IDN readIDN(DataInputStream in) throws IOException {
		int num = in.readInt();
		return num < 0 ? null : new IDN(num);
	}

	static void writeString(String s, DataOutputStream out) throws IOException {
		if (s == null) {
			out.writeInt(-1);
		} else {
			byte[] bytes = s.getBytes(UTF8);
			out.writeInt(bytes.length);
			out.write(bytes);
		}
	}

	static String readString(DataInputStream in) throws IOException {
		int len = in.readInt();
		if (len < 0)
			return null;
		byte[] bytes = new byte[len];
		in.read(bytes);
		return new String(bytes, UTF8);
	}
}
