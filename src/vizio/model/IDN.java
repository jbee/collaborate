package vizio.model;

/**
 * An {@link IDN} is an identity number that is unique for a {@link Product} (but not globally unique!).
 *
 * @author jan
 */
public final class IDN implements Comparable<IDN> {

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
		return Name.as("no_"+num);
	}
}
