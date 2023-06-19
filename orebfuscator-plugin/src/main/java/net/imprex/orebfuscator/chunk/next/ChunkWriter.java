package net.imprex.orebfuscator.chunk.next;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.accessor.BlockBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class ChunkWriter {

	private static int sectionPacketSize(Palette palette, int blockCount, int suffixLength) {
		return (ChunkCapabilities.hasBlockCount() ? 3 : 1)
				+ palette.packetSize()
				+ BlockBitStorage.packetSize(palette.bitsPerEntry())
				+ suffixLength;
	}

	private final ByteBuf outputBuffer;
	private final ChunkSegment[] sections;

	private int nextSectionIndex = 0;

	public ChunkWriter(ByteBuf outputBuffer, ChunkSegment[] sections) {
		this.outputBuffer = outputBuffer;
		this.sections = sections;
	}

	public boolean hasNext() {
		return this.nextSectionIndex < this.sections.length;
	}

	public int getSectionIndex() {
		return this.nextSectionIndex - 1;
	}

	public ChunkSection getOrWriteNext() {
		ChunkSegment sectionHolder = this.sections[this.nextSectionIndex++];
		// skip non-present chunks (e.g. heightMask)
		if (sectionHolder == null) {
			return null;
		}

		// write section buffer if present and skip processing
		if (!sectionHolder.needsProcessing()) {
			this.outputBuffer.writeBytes(sectionHolder.getBuffer());
			return null;
		}

		// always write empty sections without processing
		ChunkSection chunkSection = sectionHolder.getSection();
		if (chunkSection != null && chunkSection.isEmpty()) {
			this.writeEmpty(chunkSection.suffix);
			return null;
		}

		return chunkSection;
	}

	public void writeSection(Palette palette, int blockCount, int[] blockStates, ByteBuf suffix) {
		// make sure buffer has enough space
		int packetSize = sectionPacketSize(palette, blockCount, suffix.readableBytes());
		this.outputBuffer.ensureWritable(packetSize);

		// write optional block count
		if (ChunkCapabilities.hasBlockCount()) {
			this.outputBuffer.writeShort(blockCount);
		}

		// write bitsPerBlock + palette
		this.outputBuffer.writeByte(palette.bitsPerEntry());
		palette.write(this.outputBuffer);

		// write block data
		BitStorage.Writer blockWriter = BlockBitStorage.write(this.outputBuffer, palette);
		if (palette.bitsPerEntry() > 0) {
			for (int i = 0; i < 4096; i++) {
				blockWriter.write(blockStates[i]);
			}
			blockWriter.throwIfNotExhausted();
		}

		// append remaining suffix
		this.outputBuffer.writeBytes(suffix);
	}

	private void writeEmpty(ByteBuf suffix) {
		// write default empty section
		this.outputBuffer.writeBytes(ChunkCapabilities.emptyChunkSection());

		// append remaining suffix
		this.outputBuffer.writeBytes(suffix);
	}
}
