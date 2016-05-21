package vizio.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import vizio.Date;
import vizio.IDN;
import vizio.Name;
import vizio.Names;

public interface Streamable<T> {

	Charset UTF8 = Charset.forName("UTF-8");

	T read(DataInputStream in, PersistenceManager pm) throws IOException;

	void write(T data, DataOutputStream out) throws IOException;

	// stores
	// Product: /<product>/product.dat
	// Area:    /<product>/area/<area>.dat
	// Poll:    /<product>/poll/<area>/<matter>/<affected>.dat
	// Version: /<product>/version/<version>.dat
	// Task:    /<product>/task/<IDN>.dat

	static Name readName(DataInputStream in) throws IOException {
		int len = in.readUnsignedByte();
		byte[] name = new byte[len];
		in.read(name);
		return Name.fromBytes(name);
	}

	static Names readNames(DataInputStream in) throws IOException {
		Names names = Names.empty();
		int c = in.readUnsignedShort();
		for (int i = 0; i < c; i++) {
			names.add(readName(in));
		}
		return names;
	}

	static void writeName(Name name, DataOutputStream out) throws IOException {
		byte[] bytes = name.bytes();
		out.writeByte(bytes.length);
		out.write(bytes);
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

	static void writeDate(Date start, DataOutputStream out) throws IOException {
		out.writeInt(start.daysSinceEra);
	}

	static Date readDate(DataInputStream in) throws IOException {
		return new Date(in.readInt());
	}

	static void writeIDN(IDN id, DataOutputStream out) throws IOException {
		out.writeInt(id.num);
	}

	static IDN readIDN(DataInputStream in) throws IOException {
		return new IDN(in.readInt());
	}
}
