package net.imprex.orebfuscator.chunk.next.bitstorage.accessor;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.CompactBitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.EmptyBitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.SimpleBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class BlockBitStorage {

	private static final int SIZE = 4096;

	public static void skip(ByteBuf buffer, int bitsPerEntry) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			EmptyBitStorage.skip(buffer);
		} else if (ChunkCapabilities.hasSimpleVarBitBuffer()) {
			SimpleBitStorage.skip(buffer);
		} else {
			CompactBitStorage.skip(buffer);
		}
	}

	public static BitStorage read(ByteBuf buffer, Palette palette) {
		int bitsPerEntry = palette.bitsPerEntry();
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			return EmptyBitStorage.read(buffer, SIZE, palette);
		} else if (ChunkCapabilities.hasSimpleVarBitBuffer()) {
			return SimpleBitStorage.read(buffer, SIZE, palette);
		} else {
			return CompactBitStorage.read(buffer, SIZE, palette);
		}
	}

	public static int packetSize(int bitsPerEntry) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			return EmptyBitStorage.packetSize();
		} else if (ChunkCapabilities.hasSimpleVarBitBuffer()) {
			return SimpleBitStorage.packetSize(bitsPerEntry, SIZE);
		} else {
			return CompactBitStorage.packetSize(bitsPerEntry, SIZE);
		}
	}

	public static BitStorage.Writer write(ByteBuf buffer, Palette palette) {
		int bitsPerEntry = palette.bitsPerEntry();
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			return new EmptyBitStorage.Writer(buffer);
		} else if (ChunkCapabilities.hasSimpleVarBitBuffer()) {
			return new SimpleBitStorage.Writer(buffer, SIZE, palette);
		} else {
			return new CompactBitStorage.Writer(buffer, SIZE, palette);
		}
	}
}
