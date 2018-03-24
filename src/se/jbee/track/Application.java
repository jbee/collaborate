package se.jbee.track;

import java.util.IdentityHashMap;
import java.util.Map;

import org.lmdbjava.Env;

import se.jbee.track.api.CachedViewService;
import se.jbee.track.api.ListView;
import se.jbee.track.api.SampleView;
import se.jbee.track.api.UserInterface;
import se.jbee.track.api.ViewService;
import se.jbee.track.cache.Cache;
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
 * A place for assembling the tracker domain application.
 *
 * Different front-ends might be added to it like a HTTP web server for a HTML
 * based user interface. Another could be a command line user interface.
 *
 * This class directly or indirectly depends on all sub-packages but nothing
 * depends on it.
 */
public final class Application {

	/**
	 * Start the collaborate application with the given command line arguments.
	 *
	 * @see Server#parse(String...) for args
	 * @param args
	 *            see {@link Server#parse(String...)}, can be used without any
	 *            arguments for development
	 * @throws Exception
	 *             on problems to open DB or start the web server
	 */
	public static void main(String[] args) throws Exception {
		Server config = Server.parse(args);
		config = config.with(config.pathDB); // force check and creation of dir
		try (DB db = createDB(config)) {
			try (Cache cache = new CacheCluster(db, config.clock)) {
				ViewService views = new CachedViewService(config, db, cache);
				UserInterface ui = createHttpUserInterface(views);
				org.eclipse.jetty.server.Server server = JettyHttpServer.create(config, ui);
				server.start();
				server.join();
			}
		}
	}

	public static UserInterface createHttpUserInterface(ViewService views) {
		Map<Class<?>, HtmlRenderer<?>> renderers = new IdentityHashMap<>();
		renderers.put(ListView.class, new ListViewHtmlRenderer());
		renderers.put(SampleView.class, new SampleViewHtmlRenderer());
		return new HttpUserInterface(views, renderers);
	}

	private static DB createDB(Server config) {
		return new LMDB(Env.create().setMapSize(config.sizeDB).setMaxReaders(8), config.pathDB);
	}
}
