package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.next.bitstorage.accessor.BlockBitStorage;
import net.imprex.orebfuscator.util.MinecraftVersion;

public final class ChunkCapabilities {

	// hasClientboundLevelChunkPacketData >= 18;
	// hasBiomePalettedContainer >= 1.18
	// hasSingleValuePalette >= 1.18
	// hasHeightBitMask <= 1.17
	// hasDynamicHeight >= 1.17
	// hasSimpleVarBitBuffer >= 1.16
	// hasBlockCount >= 1.14
	// hasDirectPaletteZeroLength < 1.13
	// hasLight < 1.14

	private static final boolean hasClientboundLevelChunkPacketData = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasBiomePalettedContainer = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasSingleValuePalette = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasHeightBitMask = MinecraftVersion.minorVersion() <= 17;
	private static final boolean hasDynamicHeight = MinecraftVersion.minorVersion() >= 17;
	private static final boolean hasSimpleVarBitBuffer = MinecraftVersion.minorVersion() >= 16;
	private static final boolean hasBlockCount = MinecraftVersion.minorVersion() >= 14;
	private static final boolean hasDirectPaletteZeroLength = MinecraftVersion.minorVersion() < 13;
	private static final boolean hasLightArray = MinecraftVersion.minorVersion() < 14;

	private static final byte[] emptyChunkSection = createEmptyChunkSection();

	private static byte[] createEmptyChunkSection() {
		ByteBuf buffer = Unpooled.buffer();

		// write optional block count
		if (ChunkCapabilities.hasBlockCount()) {
			buffer.writeShort(0);
		}

		if (ChunkCapabilities.hasSingleValuePalette()) {
			// write bitsPerBlock + palette
			buffer.writeByte(0);
			ByteBufUtil.writeVarInt(buffer, 0);

			// write empty block data
			ByteBufUtil.writeVarInt(buffer, 0);
		} else {
			// write min allowed bitsPerBlock
			final int bitsPerBlock = 4;
			buffer.writeByte(bitsPerBlock);

			// write palette with air entry
			ByteBufUtil.writeVarInt(buffer, 1);
			ByteBufUtil.writeVarInt(buffer, 0);

			// write empty block data
			int packetSize = BlockBitStorage.packetSize(bitsPerBlock);
			ByteBufUtil.writeVarInt(buffer, packetSize);
			buffer.skipBytes(packetSize);
		}

		return buffer
				.capacity(buffer.readableBytes())
				.array();
	}

	private ChunkCapabilities() {
	}

	public static byte[] emptyChunkSection() {
		return emptyChunkSection;
	}

	public static boolean hasClientboundLevelChunkPacketData() {
		return hasClientboundLevelChunkPacketData;
	}

	public static boolean hasBiomePalettedContainer() {
		return hasBiomePalettedContainer;
	}

	public static boolean hasSingleValuePalette() {
		return hasSingleValuePalette;
	}

	public static boolean hasHeightBitMask() {
		return hasHeightBitMask;
	}

	public static boolean hasDynamicHeight() {
		return hasDynamicHeight;
	}

	public static boolean hasSimpleVarBitBuffer() {
		return hasSimpleVarBitBuffer;
	}

	public static boolean hasBlockCount() {
		return hasBlockCount;
	}

	public static boolean hasDirectPaletteZeroLength() {
		return hasDirectPaletteZeroLength;
	}

	public static boolean hasLightArray() {
		return hasLightArray;
	}

	public static int getExtraBytes(ChunkStruct chunkStruct) {
		int extraBytes = ChunkCapabilities.hasLightArray() ? 2048 : 0;
		if (chunkStruct.isOverworld) {
			extraBytes *= 2;
		}
		return extraBytes;
	}

	public static VarBitBuffer createVarBitBuffer(int bitsPerEntry, int size) {
		if (hasSimpleVarBitBuffer) {
			return new SimpleVarBitBuffer(bitsPerEntry, size);
		}
		return new CompactVarBitBuffer(bitsPerEntry, size);
	}
}
