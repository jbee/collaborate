package vizio;

public final class IDN implements CharSequence {

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
	public int length() {
		return (int)(Math.log10(num)+1);
	}

	@Override
	public char charAt(int index) {
		return String.valueOf(num).charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		throw new UnsupportedOperationException("Do not change IDNs!");
	}
}
