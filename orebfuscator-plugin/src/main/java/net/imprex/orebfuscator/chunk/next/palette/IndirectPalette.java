package net.imprex.orebfuscator.chunk.next.palette;

import java.util.BitSet;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.next.palette.accessor.PaletteAccessor;

public class IndirectPalette implements Palette {

	public static void skip(ByteBuf buffer) {
		int size = ByteBufUtil.readVarInt(buffer);
		for (int i = 0; i < size; i++) {
			ByteBufUtil.skipVarInt(buffer);
		}
	}

	public static Palette read(ByteBuf buffer, int bitsPerEntry, PaletteAccessor paletteAccessor) {
		int size = ByteBufUtil.readVarInt(buffer);
		if (size > (1 << bitsPerEntry)) {
			throw new IllegalStateException(
					String.format("indirect palette is too big (got=%d < expected=%d)", size, (1 << bitsPerEntry)));
		}

		int[] byIndex = new int[size];
		for (int index = 0; index < size; index++) {
			byIndex[index] = ByteBufUtil.readVarInt(buffer);
		}

		return new IndirectPalette(byIndex, bitsPerEntry, paletteAccessor);
	}

	private final int bitsPerEntry;
	private final int size;

	private final BitSet byValuePresent;
	private final byte[] byValue;
	private final int[] byIndex;

	public IndirectPalette(int[] byIndex, int bitsPerEntry, PaletteAccessor paletteAccessor) {
		this.size = byIndex.length;
		this.bitsPerEntry = bitsPerEntry;

		int numberOfValues = paletteAccessor.getNumberOfUniqueValues();
		this.byValuePresent = new BitSet(numberOfValues);
		this.byValue = new byte[numberOfValues];

		this.byIndex = byIndex;

		for (int index = 0; index < byIndex.length; index++) {
			int value = byIndex[index];
			this.byValue[value] = (byte) index;
			this.byValuePresent.set(value);
		}
	}

	@Override
	public int bitsPerEntry() {
		return this.bitsPerEntry;
	}

	@Override
	public int valueFor(int index) {
		if (index < 0 || index >= this.byIndex.length) {
			throw new IndexOutOfBoundsException();
		} else {
			return this.byIndex[index];
		}
	}

	@Override
	public int indexFor(int value) {
		if (!this.byValuePresent.get(value)) {
			throw new IllegalArgumentException("value=" + value);
		}
		return this.byValue[value] & 0xFF;
	}

	@Override
	public void write(ByteBuf buffer) {
		ByteBufUtil.writeVarInt(buffer, this.size);

		for (int index = 0; index < this.size; index++) {
			ByteBufUtil.writeVarInt(buffer, this.byIndex[index]);
		}
	}

	@Override
	public int packetSize() {
		int packetSize = ByteBufUtil.getVarIntSize(this.size);

		for (int index = 0; index < this.size; index++) {
			packetSize += ByteBufUtil.getVarIntSize(this.byIndex[index]);
		}

		return packetSize;
	}

}
