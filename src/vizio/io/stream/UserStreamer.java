package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Name;
import vizio.Site;
import vizio.User;
import vizio.io.Streamer;
import vizio.state.EntityManager;

public class UserStreamer implements Streamer<User> {

	@Override
	public User read(DataInputStream in, EntityManager em) throws IOException {
		User u = new User();
		u.name = Streamer.readName(in);
		u.email = Streamer.readString(in);
		byte[] md5 = new byte[in.readUnsignedShort()];
		in.read(md5);
		u.md5 = md5;
		u.activated = in.readBoolean();
		u.sites = Streamer.readNames(in);
		u.watches = in.readInt();
		u.millisLastActive = in.readLong();
		u.xp = in.readInt();
		u.absolved = in.readInt();
		u.resolved = in.readInt();
		u.dissolved = in.readInt();
		u.millisEmphasised = in.readLong();
		u.emphasisedToday = in.readInt();
		return u;
	}

	@Override
	public void write(User u, DataOutputStream out) throws IOException {
		Streamer.writeName(u.name, out);
		Streamer.writeString(u.email, out);
		out.writeShort(u.md5.length);
		out.write(u.md5);
		out.writeBoolean(u.activated);
		Streamer.writeNames(u.sites, out);
		out.writeInt(u.watches);
		out.writeLong(u.millisLastActive);
		out.writeInt(u.xp);
		out.writeInt(u.absolved);
		out.writeInt(u.resolved);
		out.writeInt(u.dissolved);
		out.writeLong(u.millisEmphasised);
		out.writeInt(u.emphasisedToday);
	}

}
