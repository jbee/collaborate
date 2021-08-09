package se.jbee.task.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.util.Arrays.copyOfRange;

import java.nio.charset.Charset;

import se.jbee.task.util.Array;

public abstract class Text<T extends Text<T>> implements ByteSequence<T> {

	protected final byte[] utf16symbols;
	protected final int start;
	protected final int end;

	protected Text(byte[] utf16Symbols, int start, int end) {
		this.utf16symbols = utf16Symbols;
		this.start = start;
		this.end = end;
	}

	@Override
	public final byte[] readonlyBytes() {
		return start == 0 && end == utf16symbols.length ? utf16symbols : copyOfRange(utf16symbols, start, end);
	}

	@Override
	public final Charset charset() {
		return UTF_16BE;
	}

	@Override
	public final int length() {
		return (end - start) / 2;
	}

	@Override
	public final char charAt(int index) {
		int offset = start + (index * 2);
		return utf16symbols[offset] == 0 ? (char)utf16symbols[offset+1] : UTF_16BE.decode(wrap(utf16symbols, offset, 2)).get();
	}

	@Override
	public final String toString() {
		return new String(utf16symbols, start, end - start, UTF_16BE);
	}

	@Override
	public final int hashCode() {
		return Array.hashCode(utf16symbols, start, end - start);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass() && equalTo((T) obj);
	}

	public final boolean contains(T section) {
		return ByteSequence.contains(this, section);
	}
}
