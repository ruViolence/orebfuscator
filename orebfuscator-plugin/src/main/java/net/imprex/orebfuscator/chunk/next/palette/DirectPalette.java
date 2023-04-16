package net.imprex.orebfuscator.chunk.next.palette;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.palette.accessor.PaletteAccessor;

public class DirectPalette implements Palette {

	public static void skip(ByteBuf buffer) {
		if (ChunkCapabilities.hasDirectPaletteZeroLength()) {
			ByteBufUtil.skipVarInt(buffer);
		}
	}

	private final int bitsPerEntry;

	public DirectPalette(PaletteAccessor paletteAccessor) {
		this.bitsPerEntry = paletteAccessor.getMaxBitsPerEntry();
	}

	@Override
	public int bitsPerEntry() {
		return this.bitsPerEntry;
	}

	@Override
	public int valueFor(int index) {
		return index;
	}

	@Override
	public int indexFor(int value) {
		return value;
	}

	@Override
	public void write(ByteBuf buffer) {
		if (ChunkCapabilities.hasDirectPaletteZeroLength()) {
			ByteBufUtil.writeVarInt(buffer, 0);
		}
	}

	@Override
	public int packetSize() {
		return ChunkCapabilities.hasDirectPaletteZeroLength() ? 1 : 0;
	}

}
