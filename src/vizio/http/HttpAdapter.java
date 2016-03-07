package vizio.http;

import java.io.PrintWriter;
import java.util.Map;

public interface HttpAdapter {

	/**
	 * Responds the request by writing to output stream.
	 *
	 * @param path <samp>/</samp>, <samp>/foo/</samp>, ...
	 * @param params
	 * @param out
	 * @return HTTP status code
	 */
	int respond(String path, Map<String, String> params, PrintWriter out);

}