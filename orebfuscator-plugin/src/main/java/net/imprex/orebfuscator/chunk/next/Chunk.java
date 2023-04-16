package net.imprex.orebfuscator.chunk.next;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.HeightAccessor;

public class Chunk implements AutoCloseable {

	public static Chunk	fromChunkStruct(ChunkStruct chunkStruct, WorldConfigBundle bundle) {
		return new Chunk(chunkStruct, bundle, ChunkCapabilities.getExtraBytes(chunkStruct));
	}

	private final int chunkX;
	private final int chunkZ;

	private final HeightAccessor heightAccessor;
	private final ChunkSectionHolder[] sections;

	private final ByteBuf inputBuffer;
	public final ByteBuf outputBuffer;

	private Chunk(ChunkStruct chunkStruct, WorldConfigBundle bundle, int suffixLength) {
		this.chunkX = chunkStruct.chunkX;
		this.chunkZ = chunkStruct.chunkZ;

		this.heightAccessor = HeightAccessor.get(chunkStruct.world);
		this.sections = new ChunkSectionHolder[this.heightAccessor.getSectionCount()];

		this.inputBuffer = Unpooled.wrappedBuffer(chunkStruct.data);
		this.outputBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(chunkStruct.data.length);

		for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
			if (chunkStruct.sectionMask.get(sectionIndex)) {
				ChunkSectionHolder sectionHolder;

				if (bundle.skipReadSectionIndex(sectionIndex)) {
					sectionHolder = new ChunkSectionHolder(ChunkSection.skip(inputBuffer, suffixLength));
				} else {
					sectionHolder = new ChunkSectionHolder(new ChunkSection(inputBuffer, suffixLength));
				}

				this.sections[sectionIndex] = sectionHolder;
			}
		}
	}

	public int getSectionCount() {
		return this.sections.length;
	}

	public HeightAccessor getHeightAccessor() {
		return heightAccessor;
	}

	public ChunkSection getSection(int index) {
		ChunkSectionHolder chunkSection = this.sections[index];
		if (chunkSection != null) {
			return chunkSection.section;
		}
		return null;
	}

	public ByteBuf getSectionBuffer(int index) {
		ChunkSectionHolder chunkSection = this.sections[index];
		if (chunkSection != null) {
			return chunkSection.sectionBuffer;
		}
		return null;
	}

	public int getBlockState(int x, int y, int z) {
		if (x >> 4 == this.chunkX && z >> 4 == this.chunkZ) {
			ChunkSectionHolder chunkSection = this.sections[this.heightAccessor.getSectionIndex(y)];
			if (chunkSection != null && chunkSection.section != null) {
				return chunkSection.section.getBlockState(x & 0xF, y & 0xF, z & 0xF);
			}
			return 0;
		}
		return -1;
	}

	public byte[] finalizeOutput() {
		this.outputBuffer.writeBytes(this.inputBuffer);
		return Arrays.copyOfRange(this.outputBuffer.array(), this.outputBuffer.arrayOffset(),
				this.outputBuffer.arrayOffset() + this.outputBuffer.readableBytes());
	}

	@Override
	public void close() throws Exception {
		this.inputBuffer.release();
		this.outputBuffer.release();
	}

	private class ChunkSectionHolder {

		public final ChunkSection section;
		public final ByteBuf sectionBuffer;

		public ChunkSectionHolder(ChunkSection section) {
			this.section = section;
			this.sectionBuffer = null;
		}

		public ChunkSectionHolder(ByteBuf sectionBuffer) {
			this.section = null;
			this.sectionBuffer = sectionBuffer;
		}

	}
}
