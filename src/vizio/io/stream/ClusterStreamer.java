package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Cluster;
import vizio.io.EntityManager;
import vizio.io.Streamer;

public class ClusterStreamer implements Streamer<Cluster> {

	@Override
	public Cluster read(DataInputStream in, EntityManager em) throws IOException {
		Cluster c = new Cluster(Streamer.readString(in));
		c.millisExtended = in.readLong();
		c.extensionsToday = in.readInt();
		c.millisRegistered = in.readLong();
		c.unconfirmedRegistrationsToday = in.readInt();
		return c;
	}

	@Override
	public void write(Cluster c, DataOutputStream out) throws IOException {
		Streamer.writeString(c.salt, out);
		out.writeLong(c.millisExtended);
		out.writeInt(c.extensionsToday);
		out.writeLong(c.millisRegistered);
		out.writeInt(c.unconfirmedRegistrationsToday);
	}

}
