package se.jbee.track.api;


/**
 * The {@link Page} API is meant for internal consumption to provide different
 * external APIs that are based on the same underlying model of running a
 * request described by {@link Params} and receiving the response in form of a
 * {@link Page}. Since different request have different responses the response
 * type is generic. It should been the request {@link Params}. Some
 * {@link Param#command}s might support different concrete {@link Page}s.
 * 
 * {@link Page}s are purely data records.
 * 
 * @author jan
 */
@FunctionalInterface
public interface PageService {

	<T extends Page> T run(Params request, Class<T> response) throws PageNotAvailable;
}
