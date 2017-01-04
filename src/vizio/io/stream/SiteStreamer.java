package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vizio.Name;
import vizio.Site;
import vizio.engine.EntityManager;
import vizio.io.Streamer;

public class SiteStreamer implements Streamer<Site> {

	@Override
	public Site read(DataInputStream in, EntityManager em) throws IOException {
		Name owner = Streamer.readName(in);
		Name name = Streamer.readName(in);
		String template = Streamer.readString(in);
		return new Site(owner, name, template);
	}

	@Override
	public void write(Site site, DataOutputStream out) throws IOException {
		Streamer.writeName(site.owner, out);
		Streamer.writeName(site.name, out);
		Streamer.writeString(site.template, out);		
	}

}
