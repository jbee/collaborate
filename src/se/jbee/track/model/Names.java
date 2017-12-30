package se.jbee.track.model;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;

import java.util.Arrays;
import java.util.Iterator;

import se.jbee.track.util.Array;

/**
 * A sorted set of {@link Name}s.
 * 
 * While it is sorted order must not have a particular meaning. It is the order
 * names were added.
 */
public class Names implements Iterable<Name>, Comparable<Names> {

	private static final Name[] EMPTY = new Name[0];

	private Name[] names;

	public Names(Name... names) {
		super();
		this.names = names;
	}
	
	public Name first() {
		return names[0];
	}

	public int count() {
		return names.length;
	}

	private int indexOf(User user) {
		return indexOf(user.alias);
	}

	private int indexOf(Name name) {
		return Array.indexOf(names, name, Name::equalTo);
	}

	public Names remove(Name user) {
		return wrap(Array.remove(names, user, Name::equalTo));
	}

	public Names remove(User user) {
		return remove(user.alias);
	}

	public Names add(Name user) {
		return wrap(Array.add(names, user, Name::equalTo));
	}

	private Names wrap(Name[] res) {
		return res == names ? this : new Names(res);
	}

	public Names add(User user) {
		return add(user.alias);
	}

	public boolean contains(User user) {
		return indexOf(user) >= 0;
	}

	public boolean contains(Name user) {
		return indexOf(user) >= 0;
	}

	public static Names empty() {
		return new Names(EMPTY);
	}

	@Override
	public String toString() {
		return Arrays.toString(names);
	}

	@Override
	public Iterator<Name> iterator() {
		return asList(names).iterator();
	}

	public boolean isEmpty() {
		return names.length == 0;
	}

	@Override
	public int compareTo(Names other) {
		return Array.compare(names, other.names);
	}

	public Names union(Names other) {
		Name[] res = copyOf(names, names.length+other.names.length);
		int k = names.length;
		for (Name n : other.names)
			if (!contains(n))
				res[k++] = n;
		return wrap(copyOf(res, k));
	}

}
