package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Cluster;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class ClusterStream implements Streamable<Cluster> {

	@Override
	public Cluster read(DataInputStream in, PersistenceManager pm) throws IOException {
		Cluster c = new Cluster();
		byte[] salt = new byte[in.readUnsignedShort()];
		in.read(salt);
		c.salt = new String(salt, UTF8);
		c.millisExtended = in.readLong();
		c.extensionsToday = in.readInt();
		c.millisRegistered = in.readLong();
		c.unconfirmedRegistrationsToday = in.readInt();
		return c;
	}

	@Override
	public void write(Cluster c, DataOutputStream out) throws IOException {
		byte[] salt = c.salt.getBytes(UTF8);
		out.writeShort(salt.length);
		out.write(salt);
		out.writeLong(c.millisExtended);
		out.writeInt(c.extensionsToday);
		out.writeLong(c.millisRegistered);
		out.writeInt(c.unconfirmedRegistrationsToday);
	}

}
