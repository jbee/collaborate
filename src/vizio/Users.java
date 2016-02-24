package vizio;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;

public class Users {

	private int[] ids;

	public Users(int[] ids) {
		super();
		this.ids = ids;
	}

	private int indexOf(User user) {
		int id = user.id.id;
		return indexOf(id);
	}

	private int indexOf(int id) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id)
				return i;
		}
		return -1;
	}

	public void remove(User user) {
		int idx = indexOf(user);
		if (idx >= 0) {
			if (idx == 0) {
				ids = copyOfRange(ids, 1, ids.length);
			} else {
				int[] tmp = copyOf(ids, ids.length-1);
				if (idx < ids.length-1) {
					arraycopy(ids, idx+1, tmp, idx, ids.length-idx-1);
				}
				ids=tmp;
			}
		}
	}

	public void add(User user) {
		if (indexOf(user) < 0) {
			ids = copyOf(ids, ids.length+1);
			ids[ids.length-1] = user.id.id;
		}
	}

	public boolean contains(User user) {
		return indexOf(user) >= 0;
	}

}
