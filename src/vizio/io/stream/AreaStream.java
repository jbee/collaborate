package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Area;
import vizio.Motive;
import vizio.Purpose;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class AreaStream implements Streamable<Area> {

	@Override
	public Area read(DataInputStream in, PersistenceManager pm) throws IOException {
		Area a = new Area();
		a.product = Streamable.readName(in);
		a.name = Streamable.readName(in);
		a.basis = Streamable.readName(in);
		a.maintainers = Streamable.readNames(in);
		a.tasks = new AtomicInteger(in.readInt());
		a.exclusive = in.readBoolean();
		a.entrance = in.readBoolean();
		a.motive = Streamable.readEnum(Motive.class, in);
		a.purpose = Streamable.readEnum(Purpose.class, in);
		return a;
	}

	@Override
	public void write(Area a, DataOutputStream out) throws IOException {
		Streamable.writeName(a.product, out);
		Streamable.writeName(a.name, out);
		Streamable.writeName(a.basis, out);
		Streamable.writeNames(a.maintainers, out);
		out.writeInt(a.tasks.get());
		out.writeBoolean(a.exclusive);
		out.writeBoolean(a.entrance);
		Streamable.writeEnum(a.motive, out);
		Streamable.writeEnum(a.purpose, out);
	}

}
