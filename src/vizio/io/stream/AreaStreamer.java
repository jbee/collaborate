package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.engine.EntityManager;
import vizio.io.Streamer;
import vizio.model.Area;
import vizio.model.Motive;
import vizio.model.Purpose;

public class AreaStreamer implements Streamer<Area> {

	@Override
	public Area read(DataInputStream in, EntityManager em) throws IOException {
		Area a = new Area();
		a.product = Streamer.readName(in);
		a.name = Streamer.readName(in);
		a.basis = Streamer.readName(in);
		a.maintainers = Streamer.readNames(in);
		a.polls = in.readInt();
		a.tasks = in.readInt();
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
		out.writeInt(a.polls);
		out.writeInt(a.tasks);
		out.writeBoolean(a.exclusive);
		out.writeBoolean(a.entrance);
		Streamer.writeEnum(a.motive, out);
		Streamer.writeEnum(a.purpose, out);
	}

}
