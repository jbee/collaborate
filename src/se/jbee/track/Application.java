package se.jbee.track;

import java.util.IdentityHashMap;
import java.util.Map;

import org.lmdbjava.Env;

import se.jbee.track.api.CachedViewService;
import se.jbee.track.api.ListView;
import se.jbee.track.api.SampleView;
import se.jbee.track.api.UserInterface;
import se.jbee.track.cache.CacheCluster;
import se.jbee.track.db.DB;
import se.jbee.track.db.LMDB;
import se.jbee.track.engine.Server;
import se.jbee.track.html.HtmlRenderer;
import se.jbee.track.html.ListViewHtmlRenderer;
import se.jbee.track.html.SampleViewHtmlRenderer;
import se.jbee.track.http.HttpUserInterface;
import se.jbee.track.http.JettyHttpServer;

/**
 * A place for assembling the logical tracker application.
 *
 * Different front-ends  might be added to it like a HTTP web server for a HTML based user interface.
 * Another could be a command line user interface.
 *
 * @author jan
 */
public final class Application {

	public static void main(String[] args) throws Exception {
		Server config = Server.parse(args);
		UserInterface ui = createHttpUserInterface(config);
		JettyHttpServer.start(config, ui);
	}

	public static UserInterface createHttpUserInterface(Server config) {
		Map<Class<?>, HtmlRenderer<?>> renderers = new IdentityHashMap<>();
		renderers.put(ListView.class, new ListViewHtmlRenderer());
		renderers.put(SampleView.class, new SampleViewHtmlRenderer());
		DB db = createDB(config);
		return new HttpUserInterface(new CachedViewService(config, db, new CacheCluster(db, config.clock)), renderers);
	}

	private static DB createDB(Server config) {
		LMDB db = new LMDB(Env.create().setMapSize(config.sizeDB).setMaxReaders(8), config.pathDB);
		Runtime.getRuntime().addShutdownHook(new Thread(() ->  db.close() ));
		return db;
	}
}
