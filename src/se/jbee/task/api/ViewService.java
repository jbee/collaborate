package se.jbee.task.api;


/**
 * The {@link View} API is meant for internal consumption to provide different
 * external APIs that are based on the same underlying model of running a
 * request described by {@link Params} and receiving the response in form of a
 * {@link View}. Since different request have different responses the response
 * type is generic. It should been the request {@link Params}. Some
 * {@link Param#command}s might support different concrete {@link View}s.
 * 
 * {@link View}s are purely data records.
 * 
 * @author jan
 */
@FunctionalInterface
public interface ViewService {

	<T extends View> T run(Params request, Class<T> response) throws ViewNotAvailable;
}
