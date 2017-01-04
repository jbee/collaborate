package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.engine.EntityManager;
import vizio.io.Streamer;
import vizio.model.Motive;
import vizio.model.Purpose;
import vizio.model.Status;
import vizio.model.Task;

public class TaskStreamer implements Streamer<Task> {

	@Override
	public Task read(DataInputStream in, EntityManager em) throws IOException {
		Task t = new Task();
		t.product = em.product(Streamer.readName(in));
		t.area = em.area(t.product.name, Streamer.readName(in));
		t.id = Streamer.readIDN(in);
		t.serial = Streamer.readIDN(in);
		t.reporter = Streamer.readName(in);
		t.start = Streamer.readDate(in);
		t.gist = Streamer.readString(in);
		t.motive = Streamer.readEnum(Motive.class, in);
		t.purpose = Streamer.readEnum(Purpose.class, in);
		t.status = Streamer.readEnum(Status.class, in);
		t.changeset = Streamer.readNames(in);
		t.exploitable = in.readBoolean();
		t.cause = Streamer.readIDN(in);
		t.origin = Streamer.readIDN(in);
		t.heat = in.readInt();
		t.base = em.version(t.product.name, Streamer.readName(in));
		t.enlistedBy = Streamer.readNames(in);
		t.approachedBy = Streamer.readNames(in);
		t.watchedBy = Streamer.readNames(in);
		t.solver = Streamer.readName(in);
		t.end = Streamer.readDate(in);
		t.conclusion = Streamer.readString(in);
		return t;
	}

	@Override
	public void write(Task t, DataOutputStream out) throws IOException {
		Streamer.writeName(t.product.name, out);
		Streamer.writeName(t.area.name, out);
		Streamer.writeIDN(t.id, out);
		Streamer.writeIDN(t.serial, out);
		Streamer.writeName(t.reporter, out);
		Streamer.writeDate(t.start, out);
		Streamer.writeString(t.gist, out);
		Streamer.writeEnum(t.motive, out);
		Streamer.writeEnum(t.purpose, out);
		Streamer.writeEnum(t.status, out);
		Streamer.writeNames(t.changeset, out);
		out.writeBoolean(t.exploitable);
		Streamer.writeIDN(t.cause, out);
		Streamer.writeIDN(t.origin, out);
		out.writeInt(t.heat);
		Streamer.writeName(t.base.name, out);
		Streamer.writeNames(t.enlistedBy, out);
		Streamer.writeNames(t.approachedBy, out);
		Streamer.writeNames(t.watchedBy, out);
		Streamer.writeName(t.solver, out);
		Streamer.writeDate(t.end, out);
		Streamer.writeString(t.conclusion, out);
	}

}
