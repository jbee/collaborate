package vizio.model;

import static java.util.Arrays.asList;

import java.util.Iterator;
import java.util.Set;

public final class Attachments implements Iterable<URL>, Comparable<Attachments> {

	public static final Attachments NONE = new Attachments(new URL[0]);

	public static Attachments attachments(URL...urls) {
		return urls == null || urls.length == 0 ? NONE : new Attachments(urls);
	}
	
	private final URL[] urls;

	private Attachments(URL[] urls) {
		super();
		this.urls = urls;
	}

	@Override
	public Iterator<URL> iterator() {
		return asList(urls).iterator();
	}

	@Override
	public int compareTo(Attachments other) {
		int res = urls.length - other.urls.length;
		if (res == 0) { // compare as sets
			for (URL u : other.urls) {
				if (!contains(u))
					return 1;
			}
			return 0;
		}
		return res;
	}
	
	public boolean contains(URL url) {
		for (URL u : urls) {
			if (u.equalTo(url))
				return true;
		}
		return false;
	}

	public int length() {
		return urls.length;
	}
	
}
