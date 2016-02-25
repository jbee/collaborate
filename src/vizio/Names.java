package vizio;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

public class Names {

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

	public void remove(User user) {
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

	public void add(User user) {
		if (indexOf(user) < 0) {
			names = copyOf(names, names.length+1);
			names[names.length-1] = user.name;
		}
	}

	public boolean contains(User user) {
		return indexOf(user) >= 0;
	}

	public static Names empty() {
		return new Names(EMPTY);
	}

}
