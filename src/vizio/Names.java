package vizio;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

import java.util.Arrays;
import java.util.Iterator;

public class Names implements Iterable<Name>, Comparable<Names> {

	private static final Name[] EMPTY = new Name[0];

	private Name[] names;

	public Names(Name... names) {
		super();
		this.names = names;
	}

	public int count() {
		return names.length;
	}

	private int indexOf(User user) {
		return indexOf(user.name);
	}

	private int indexOf(Name name) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(name))
				return i;
		}
		return -1;
	}

	public Names remove(Name user) {
		int idx = indexOf(user);
		if (idx >= 0) {
			if (idx == 0) {
				return new Names(copyOfRange(names, 1, names.length));
			}
			Name[] tmp = copyOf(names, names.length-1);
			if (idx < names.length-1) {
				arraycopy(names, idx+1, tmp, idx, names.length-idx-1);
			}
			return new Names(tmp);
		}
		return this;
	}

	public Names remove(User user) {
		return remove(user.name);
	}

	public Names add(Name user) {
		if (indexOf(user) < 0) {
			Name[] res = copyOf(names, names.length+1);
			res[names.length] = user;
			return new Names(res);
		}
		return this;
	}

	public Names add(User user) {
		return add(user.name);
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
		int cmp = Integer.compare(names.length, other.names.length);
		if (cmp != 0)
			return cmp;
		for (int i = 0; i < names.length; i++) {
			cmp = names[i].compareTo(other.names[i]);
			if (cmp != 0)
				return cmp;
		}
		return 0;
	}

}
