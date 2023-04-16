package net.imprex.orebfuscator.chunk.next;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.accessor.BiomeBitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.accessor.BlockBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.palette.accessor.BiomePaletteAccessor;
import net.imprex.orebfuscator.chunk.next.palette.accessor.BlockPaletteAccessor;

public class ChunkSection {

	static int positionToIndex(int x, int y, int z) {
		return y << 8 | z << 4 | x;
	}

	static ByteBuf skip(ByteBuf buffer, int suffixLength) {
		int readerIndex = buffer.readerIndex();

		if (ChunkCapabilities.hasBlockCount()) {
			buffer.skipBytes(2);
		}

		int bitsPerBlock = buffer.readUnsignedByte();
		BlockPaletteAccessor.skip(buffer, bitsPerBlock);
		BlockBitStorage.skip(buffer, bitsPerBlock);

		if (ChunkCapabilities.hasBiomePalettedContainer()) {
			int bitsPerBiome = buffer.readUnsignedByte();
			BiomePaletteAccessor.skip(buffer, bitsPerBiome);
			BiomeBitStorage.skip(buffer, bitsPerBiome);
		} else {
			buffer.skipBytes(suffixLength);
		}

		int sectionLength = buffer.readerIndex() - readerIndex;
		return buffer.slice(readerIndex, sectionLength);
	}

	public final int blockCount;
	public final int bitsPerBlock;

	public final Palette blockPalette;
	public final BitStorage blockBitStorage;

	public final ByteBuf suffix;

	public ChunkSection(ByteBuf buffer, int suffixLength) {
		if (ChunkCapabilities.hasBlockCount()) {
			this.blockCount = buffer.readShort();
		} else {
			this.blockCount = -1;
		}

		// TODO skip read if empty
		this.bitsPerBlock = buffer.readUnsignedByte();
		this.blockPalette = BlockPaletteAccessor.read(buffer, this.bitsPerBlock);
		this.blockBitStorage = BlockBitStorage.read(buffer, this.blockPalette);

		if (ChunkCapabilities.hasBiomePalettedContainer()) {
			int suffixOffset = buffer.readerIndex();

			int bitsPerBiome = buffer.readUnsignedByte();
			BiomePaletteAccessor.skip(buffer, bitsPerBiome);
			BiomeBitStorage.skip(buffer, bitsPerBiome);

			suffixLength = buffer.readerIndex() - suffixOffset;
			this.suffix = buffer.slice(suffixOffset, suffixLength);
		} else {
			this.suffix = buffer.readSlice(suffixLength);
		}
	}

	public boolean isEmpty() {
		return ChunkCapabilities.hasBlockCount() && this.blockCount == 0;
	}

	public int getBlockState(int x, int y, int z) {
		return getBlockState(positionToIndex(x, y, z));
	}

	public int getBlockState(int index) {
		return this.blockBitStorage.get(index);
	}
}
