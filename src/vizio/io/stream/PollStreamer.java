package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.IDN;
import vizio.Outcome;
import vizio.Poll;
import vizio.Poll.Matter;
import vizio.io.EntityManager;
import vizio.io.Streamer;

public class PollStreamer implements Streamer<Poll> {

	@Override
	public Poll read(DataInputStream in, EntityManager em) throws IOException {
		Poll p = new Poll();
		p.serial = new IDN(in.readInt());
		p.area = em.area(Streamer.readName(in), Streamer.readName(in));
		p.matter = Streamer.readEnum(Matter.class, in);
		p.affected = em.user(Streamer.readName(in));
		p.initiator = Streamer.readName(in);
		p.start = Streamer.readDate(in);
		p.consenting = Streamer.readNames(in);
		p.dissenting = Streamer.readNames(in);
		p.expiry = Streamer.readDate(in);
		p.end = Streamer.readDate(in);
		p.outcome = Streamer.readEnum(Outcome.class, in);
		return p;
	}

	@Override
	public void write(Poll p, DataOutputStream out) throws IOException {
		Streamer.writeIDN(p.serial, out);
		Streamer.writeName(p.area.product, out);
		Streamer.writeName(p.area.name, out);
		Streamer.writeEnum(p.matter, out);
		Streamer.writeName(p.affected.name, out);
		Streamer.writeName(p.initiator, out);
		Streamer.writeDate(p.start, out);
		Streamer.writeNames(p.consenting, out);
		Streamer.writeNames(p.dissenting, out);
		Streamer.writeDate(p.expiry, out);
		Streamer.writeDate(p.end, out);
		Streamer.writeEnum(p.outcome, out);
	}

}
