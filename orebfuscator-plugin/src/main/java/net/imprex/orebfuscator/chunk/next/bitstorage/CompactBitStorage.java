package net.imprex.orebfuscator.chunk.next.bitstorage;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class CompactBitStorage implements BitStorage {

	private static int expectedLength(int size, int bitsPerEntry) {
		return (int) Math.ceil(bitsPerEntry * size / 64f);
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
			// fix: FAWE chunk format incompatibility
			// https://github.com/Imprex-Development/Orebfuscator/issues/36
			buffer.skipBytes(Long.BYTES * length);
			return new EmptyBitStorage(size, palette.valueFor(0));
		}

		int[] values = new int[size];
		int valueMask = (1 << palette.bitsPerEntry()) - 1;

		int bitOffset = 0;
		long longBuffer = buffer.readLong();

		for (int index = 0; index < values.length; index++) {
			int paletteIndex = (int) (longBuffer >>> bitOffset);
			bitOffset += bitsPerEntry;

			if (bitOffset > 63 && index != values.length - 1) {
				longBuffer = buffer.readLong();
				bitOffset -= 64;

				if (bitOffset > 0) {
					paletteIndex |= longBuffer << (bitsPerEntry - bitOffset);
				}
			}

			values[index] = palette.valueFor(paletteIndex & valueMask);
		}

		return new CompactBitStorage(values);
	}

	public static int packetSize(int bitsPerEntry, int size) {
		int arrayLength = expectedLength(size, bitsPerEntry);
		return ByteBufUtil.getVarIntSize(arrayLength) + (arrayLength * Long.BYTES);
	}

	private final int[] values;

	private CompactBitStorage(int[] values) {
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
		private final int valueMask;
		private final int packetSize;

		private final ByteBuf buffer;
		private final Palette palette;

		private int bitOffset = 0;
		private long longBuffer = 0;

		private int capacity;

		public Writer(ByteBuf buffer, int size, Palette palette) {
			this.bitsPerEntry = palette.bitsPerEntry();
			this.packetSize = expectedLength(size, this.bitsPerEntry);

			this.capacity = size;
			this.valueMask = (1 << this.bitsPerEntry) - 1;

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
				throw new IllegalStateException("BitStorage.Writer is already exhausted!");
			}

			long valueValue = this.palette.indexFor(value) & this.valueMask;

			this.longBuffer |= valueValue << this.bitOffset;
			this.bitOffset += this.bitsPerEntry;

			if (this.bitOffset > 63) {
				this.buffer.writeLong(this.longBuffer);
				this.bitOffset -= 64;

				if (this.bitOffset > 0) {
					this.longBuffer = valueValue >>> (this.bitsPerEntry - this.bitOffset);
				} else {
					this.longBuffer = 0;
				}
			}

			if (--this.capacity == 0 && this.bitOffset != 0) {
				this.buffer.writeLong(this.longBuffer);
			}
		}
	}
}
