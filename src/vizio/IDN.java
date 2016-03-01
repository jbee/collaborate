package vizio;

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
}
