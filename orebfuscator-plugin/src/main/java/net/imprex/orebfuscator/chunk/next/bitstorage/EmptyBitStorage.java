package net.imprex.orebfuscator.chunk.next.bitstorage;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class EmptyBitStorage implements BitStorage {

	public static void skip(ByteBuf buffer) {
		ByteBufUtil.skipVarInt(buffer);
	}

	public static BitStorage read(ByteBuf buffer, int size, Palette palette) {
		if (palette.bitsPerEntry() != 0) {
			throw new IllegalArgumentException("invalid palette!");
		}

		ByteBufUtil.skipVarInt(buffer);

		return new EmptyBitStorage(size, palette.valueFor(0));
	}

	public static int packetSize() {
		return 1;
	}

	private final int size;

	private final int value;

	public EmptyBitStorage(int size, int value) {
		this.size = size;
		this.value = value;
	}

	@Override
	public int get(int index) {
		return this.value;
	}

	@Override
	public int size() {
		return this.size;
	}

	public static class Writer implements BitStorage.Writer {

		public Writer(ByteBuf buffer) {
			ByteBufUtil.writeVarInt(buffer, 0);
		}

		@Override
		public void write(int value) {
			// NOOP
		}
	}
}
