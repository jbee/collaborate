package se.jbee.track.model;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Iterator;

import se.jbee.track.util.Array;

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
		return Array.compare(urls, other.urls, URL::equalTo);
	}
	
	public boolean contains(URL url) {
		return Array.indexOf(urls, url, URL::equalTo) >= 0;
	}

	public int length() {
		return urls.length;
	}
	
	public Attachments add(URL url) {
		return wrap(Array.add(urls, url, URL::equalTo));
	}
	
	public Attachments remove(URL url) {
		return wrap(Array.remove(urls, url, URL::equalTo));
	}
	
	private Attachments wrap(URL[] res) {
		return urls == res ? this : new Attachments(res);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Attachments && equalTo((Attachments) obj);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(urls);
	}

	public boolean equalTo(Attachments other) {
		return Arrays.equals(urls, other.urls);
	}
	
}
