package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Motive;
import vizio.Purpose;
import vizio.Status;
import vizio.Task;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class TaskStream implements Streamable<Task> {

	@Override
	public Task read(DataInputStream in, PersistenceManager pm) throws IOException {
		Task t = new Task();
		t.product = pm.product(Streamable.readName(in));
		t.area = pm.area(t.product.name, Streamable.readName(in));
		t.id = Streamable.readIDN(in);
		t.serial = Streamable.readIDN(in);
		t.reporter = Streamable.readName(in);
		t.start = Streamable.readDate(in);
		byte[] gist = new byte[in.readUnsignedShort()];
		in.read(gist);
		t.gist = new String(gist, UTF8);
		t.motive = Streamable.readEnum(Motive.class, in);
		t.purpose = Streamable.readEnum(Purpose.class, in);
		t.status = Streamable.readEnum(Status.class, in);
		t.changeset = Streamable.readNames(in);
		t.exploitable = in.readBoolean();
		t.cause = Streamable.readIDN(in);
		t.origin = Streamable.readIDN(in);
		t.heat = in.readInt();
		t.base = pm.version(t.product.name, Streamable.readName(in));
		t.enlistedBy = Streamable.readNames(in);
		t.approachedBy = Streamable.readNames(in);
		t.watchedBy = Streamable.readNames(in);
		t.confirmed = in.readBoolean();
		t.solver = Streamable.readName(in);
		t.end = Streamable.readDate(in);
		byte[] conclusion = new byte[in.readInt()];
		in.read(conclusion);
		t.conclusion = new String(conclusion, UTF8);
		return t;
	}

	@Override
	public void write(Task t, DataOutputStream out) throws IOException {
		Streamable.writeName(t.product.name, out);
		Streamable.writeName(t.area.name, out);
		Streamable.writeIDN(t.id, out);
		Streamable.writeIDN(t.serial, out);
		Streamable.writeName(t.reporter, out);
		Streamable.writeDate(t.start, out);
		byte[] gist = t.gist.getBytes(UTF8);
		out.writeShort(gist.length);
		out.write(gist);
		Streamable.writeEnum(t.motive, out);
		Streamable.writeEnum(t.purpose, out);
		Streamable.writeEnum(t.status, out);
		Streamable.writeNames(t.changeset, out);
		out.writeBoolean(t.exploitable);
		Streamable.writeIDN(t.cause, out);
		Streamable.writeIDN(t.origin, out);
		out.writeInt(t.heat);
		Streamable.writeName(t.base.name, out);
		Streamable.writeNames(t.enlistedBy, out);
		Streamable.writeNames(t.approachedBy, out);
		Streamable.writeNames(t.watchedBy, out);
		out.writeBoolean(t.confirmed);
		Streamable.writeName(t.solver, out);
		Streamable.writeDate(t.end, out);
		byte[] conclusion = t.conclusion.getBytes(UTF8);
		out.writeInt(conclusion.length);
		out.write(conclusion);
	}

}
