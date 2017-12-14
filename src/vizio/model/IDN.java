package vizio.model;

/**
 * An {@link IDN} is an identity number that is unique for a {@link Product} (but not globally unique!).
 *
 * @author jan
 */
public final class IDN implements Comparable<IDN> {

	public static final IDN ZERO = new IDN(0);
	
	public final int num;

	public IDN(int num) {
		super();
		this.num = num;
	}

	@Override
	public String toString() {
		return ""+num;
	}

	@Override
	public int compareTo(IDN other) {
		return Integer.compare(num, other.num);
	}
	
	public Name asName() {
		return Name.as(String.valueOf(num));
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof IDN && equalTo((IDN) obj);
	}

	public boolean equalTo(IDN other) {
		return num == other.num;
	}
	
	@Override
	public int hashCode() {
		return num;
	}
}
