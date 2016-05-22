package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Version;
import vizio.io.PersistenceManager;
import vizio.io.Streamer;

public class VersionStreamer implements Streamer<Version> {

	@Override
	public Version read(DataInputStream in, PersistenceManager pm) throws IOException {
		Version v = new Version();
		v.product = Streamer.readName(in);
		v.name = Streamer.readName(in);
		v.changeset = Streamer.readNames(in);
		return v;
	}

	@Override
	public void write(Version v, DataOutputStream out) throws IOException {
		Streamer.writeName(v.product, out);
		Streamer.writeName(v.name, out);
		Streamer.writeNames(v.changeset, out);
	}

}
