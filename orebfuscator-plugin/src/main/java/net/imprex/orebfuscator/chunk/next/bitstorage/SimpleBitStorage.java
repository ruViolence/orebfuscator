package net.imprex.orebfuscator.chunk.next.bitstorage;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class SimpleBitStorage implements BitStorage {

	public static int expectedLength(int size, int bitsPerEntry) {
		int valuesPerLong = 64 / bitsPerEntry;
		return (size + valuesPerLong - 1) / valuesPerLong;
	}

	public static void skip(ByteBuf buffer) {
		int length = ByteBufUtil.readVarInt(buffer);
		buffer.skipBytes(length * Long.BYTES);
	}

	public static BitStorage read(ByteBuf buffer, int size, Palette palette) {
		int bitsPerEntry = palette.bitsPerEntry();
		int expectedLength = expectedLength(size, bitsPerEntry);

		int length = ByteBufUtil.readVarInt(buffer);
		if (length != expectedLength) {
			// fix: FAWE chunk format incompatibility with bitsPerEntry == 1
			// https://github.com/Imprex-Development/Orebfuscator/issues/36
			buffer.skipBytes(Long.BYTES * length);
			return new EmptyBitStorage(size, palette.valueFor(0));
		}

		int[] values = new int[size];
		int valueMask = (1 << palette.bitsPerEntry()) - 1;

		int bitOffset = 0;
		long longBuffer = buffer.readLong();

		for (int index = 0; index < values.length; index++) {
			if (bitOffset + bitsPerEntry > 64) {
				longBuffer = buffer.readLong();
				bitOffset = 0;
			}

			int paletteIndex = (int) (longBuffer >>> bitOffset) & valueMask;
			values[index] = palette.valueFor(paletteIndex);

			bitOffset += bitsPerEntry;
		}

		return new SimpleBitStorage(values);
	}

	public static int packetSize(int bitsPerEntry, int size) {
		int arrayLength = expectedLength(size, bitsPerEntry);
		return ByteBufUtil.getVarIntSize(arrayLength) + (arrayLength * Long.BYTES);
	}

	private final int[] values;

	private SimpleBitStorage(int[] values) {
		this.values = values;
	}

	@Override
	public int get(int index) {
		return this.values[index];
	}

	@Override
	public int size() {
		return this.values.length;
	}

	public static class Writer implements BitStorage.Writer {

		private final int bitsPerEntry;
		private final long valueMask;
		private final int packetSize;

		private final ByteBuf buffer;
		private final Palette palette;

		private int bitOffset = 0;
		private long longBuffer = 0;

		private int capacity;

		public Writer(ByteBuf buffer, int size, Palette palette) {
			this.bitsPerEntry = palette.bitsPerEntry();
			this.valueMask = (1L << this.bitsPerEntry) - 1L;

			this.capacity = size;
			this.packetSize = expectedLength(size, this.bitsPerEntry);

			this.buffer = buffer;
			this.palette = palette;

			ByteBufUtil.writeVarInt(buffer, this.packetSize);
		}

		@Override
		public boolean isExhausted() {
			return this.capacity == 0;
		}

		@Override
		public void write(int value) {
			if (this.capacity <= 0) {
				throw new IllegalStateException("BitStorage.Writer is already exhausted");
			}

			if (this.bitOffset + this.bitsPerEntry > 64) {
				this.buffer.writeLong(this.longBuffer);
				this.longBuffer = 0;
				this.bitOffset = 0;
			}

			value = this.palette.indexFor(value);
			longBuffer |= (value & this.valueMask) << this.bitOffset;

			this.bitOffset += this.bitsPerEntry;

			if (--this.capacity == 0) {
				this.buffer.writeLong(this.longBuffer);
			}
		}
	}
}
