package se.jbee.track.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_16BE;

import java.nio.charset.Charset;
import java.util.Arrays;

public abstract class Text<T extends Text<T>> implements ByteSequence<T> {

	private final byte[] text;

	protected Text(byte[] utf16Symbols) {
		super();
		this.text = utf16Symbols;
	}

	@Override
	public byte[] readonlyBytes() {
		return text;
	}

	@Override
	public Charset charset() {
		return UTF_16BE;
	}

	@Override
	public int length() {
		return text.length / 2;
	}

	@Override
	public char charAt(int index) {
		return charset().decode(wrap(text, index*2, 2)).get();
	}

	@Override
	public String toString() {
		return new String(text, charset());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(text);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass() && equalTo((T) obj);
	}

	public boolean contains(T section) {
		return ByteSequence.contains(this, section);
	}
}
