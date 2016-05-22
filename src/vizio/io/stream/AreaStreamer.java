package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Area;
import vizio.Motive;
import vizio.Purpose;
import vizio.io.PersistenceManager;
import vizio.io.Streamer;

public class AreaStreamer implements Streamer<Area> {

	@Override
	public Area read(DataInputStream in, PersistenceManager pm) throws IOException {
		Area a = new Area();
		a.product = Streamer.readName(in);
		a.name = Streamer.readName(in);
		a.basis = Streamer.readName(in);
		a.maintainers = Streamer.readNames(in);
		a.tasks = new AtomicInteger(in.readInt());
		a.exclusive = in.readBoolean();
		a.entrance = in.readBoolean();
		a.motive = Streamer.readEnum(Motive.class, in);
		a.purpose = Streamer.readEnum(Purpose.class, in);
		return a;
	}

	@Override
	public void write(Area a, DataOutputStream out) throws IOException {
		Streamer.writeName(a.product, out);
		Streamer.writeName(a.name, out);
		Streamer.writeName(a.basis, out);
		Streamer.writeNames(a.maintainers, out);
		out.writeInt(a.tasks.get());
		out.writeBoolean(a.exclusive);
		out.writeBoolean(a.entrance);
		Streamer.writeEnum(a.motive, out);
		Streamer.writeEnum(a.purpose, out);
	}

}
