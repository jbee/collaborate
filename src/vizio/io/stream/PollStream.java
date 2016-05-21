package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Outcome;
import vizio.Poll;
import vizio.Poll.Matter;
import vizio.io.PersistenceManager;
import vizio.io.Streamable;

public class PollStream implements Streamable<Poll> {

	@Override
	public Poll read(DataInputStream in, PersistenceManager pm) throws IOException {
		Poll p = new Poll();
		p.area = pm.area(Streamable.readName(in), Streamable.readName(in));
		p.matter = Streamable.readEnum(Matter.class, in);
		p.affected = pm.user(Streamable.readName(in));
		p.initiator = Streamable.readName(in);
		p.start = Streamable.readDate(in);
		p.consenting = Streamable.readNames(in);
		p.dissenting = Streamable.readNames(in);
		p.expiry = Streamable.readDate(in);
		p.end = Streamable.readDate(in);
		p.outcome = Streamable.readEnum(Outcome.class, in);
		return p;
	}

	@Override
	public void write(Poll p, DataOutputStream out) throws IOException {
		Streamable.writeName(p.area.product, out);
		Streamable.writeName(p.area.name, out);
		Streamable.writeEnum(p.matter, out);
		Streamable.writeName(p.affected.name, out);
		Streamable.writeName(p.initiator, out);
		Streamable.writeDate(p.start, out);
		Streamable.writeNames(p.consenting, out);
		Streamable.writeNames(p.dissenting, out);
		Streamable.writeDate(p.expiry, out);
		Streamable.writeDate(p.end, out);
		Streamable.writeEnum(p.outcome, out);
	}

}
