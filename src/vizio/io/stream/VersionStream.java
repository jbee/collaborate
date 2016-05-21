package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Version;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class VersionStream implements Streamable<Version> {

	@Override
	public Version read(DataInputStream in, PersistenceManager pm) throws IOException {
		Version v = new Version();
		v.product = Streamable.readName(in);
		v.name = Streamable.readName(in);
		v.changeset = Streamable.readNames(in);
		return v;
	}

	@Override
	public void write(Version v, DataOutputStream out) throws IOException {
		Streamable.writeName(v.product, out);
		Streamable.writeName(v.name, out);
		Streamable.writeNames(v.changeset, out);
	}

}
