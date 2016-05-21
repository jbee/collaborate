package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Name;
import vizio.Site;
import vizio.User;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class UserStream implements Streamable<User> {

	@Override
	public User read(DataInputStream in, PersistenceManager pm) throws IOException {
		User u = new User();
		u.name = Streamable.readName(in);
		byte[] email = new byte[in.readUnsignedShort()];
		in.read(email);
		u.email = new String(email, UTF8);
		byte[] md5 = new byte[in.readUnsignedShort()];
		in.read(md5);
		u.md5 = md5;
		u.activated = in.readBoolean();
		int sites = in.readUnsignedByte();
		u.sites = new Site[sites];
		for (int i = 0; i < sites; i++) {
			Name name = Streamable.readName(in);
			byte[] template = new byte[in.readInt()];
			in.read(template);
			u.sites[i] = new Site(u.name, name, new String(template, UTF8));
		}
		u.watches = new AtomicInteger(in.readInt());
		u.millisLastActive = in.readLong();
		u.xp = in.readInt();
		u.absolved = in.readInt();
		u.resolved = in.readInt();
		u.dissolved = in.readInt();
		u.millisStressed = in.readLong();
		u.stressedToday = in.readInt();
		u.millisReported = in.readLong();
		u.reportedToday = in.readInt();
		return u;
	}

	@Override
	public void write(User u, DataOutputStream out) throws IOException {
		Streamable.writeName(u.name, out);
		byte[] email = u.email.getBytes(UTF8);
		out.writeShort(email.length);
		out.write(email);
		out.writeShort(u.md5.length);
		out.write(u.md5);
		out.writeBoolean(u.activated);
		out.writeByte(u.sites.length);
		for (int i = 0; i < u.sites.length; i++) {
			Site s = u.sites[i];
			Streamable.writeName(s.name, out);
			byte[] t = s.template.getBytes(UTF8);
			out.writeInt(t.length);
			out.write(t);
		}
		out.writeInt(u.watches.get());
		out.writeLong(u.millisLastActive);
		out.writeInt(u.xp);
		out.writeInt(u.absolved);
		out.writeInt(u.resolved);
		out.writeInt(u.dissolved);
		out.writeLong(u.millisStressed);
		out.writeInt(u.stressedToday);
		out.writeLong(u.millisReported);
		out.writeInt(u.reportedToday);
	}

}
