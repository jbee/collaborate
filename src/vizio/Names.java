package vizio;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

import java.util.Arrays;
import java.util.Iterator;

public class Names implements Iterable<Name> {

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

	public void remove(Name user) {
		int idx = indexOf(user);
		if (idx >= 0) {
			if (idx == 0) {
				names = copyOfRange(names, 1, names.length);
			} else {
				Name[] tmp = copyOf(names, names.length-1);
				if (idx < names.length-1) {
					arraycopy(names, idx+1, tmp, idx, names.length-idx-1);
				}
				names=tmp;
			}
		}
	}

	public void remove(User user) {
		remove(user.name);
	}

	public void add(Name user) {
		if (indexOf(user) < 0) {
			names = copyOf(names, names.length+1);
			names[names.length-1] = user;
		}
	}

	public void add(User user) {
		add(user.name);
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

}
