package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Bukkit;
import org.bukkit.World;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.chunk.next.Chunk;
import net.imprex.orebfuscator.chunk.next.ChunkSection;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.accessor.BlockBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.palette.accessor.BlockPaletteAccessor;
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationTaskProcessor implements ObfuscationTask.Processor {

	private static final AtomicLong TIME = new AtomicLong();
	private static final AtomicInteger COUNT = new AtomicInteger(-2500);

	private final OrebfuscatorConfig config;

	public ObfuscationTaskProcessor(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	public void process(ObfuscationTask task) {
		long time = System.currentTimeMillis();

		ChunkStruct chunkStruct = task.getChunkStruct();

		World world = chunkStruct.world;
		HeightAccessor heightAccessor = HeightAccessor.get(world);

		WorldConfigBundle bundle = this.config.world(world);
		BlockFlags blockFlags = bundle.blockFlags();
		ObfuscationConfig obfuscationConfig = bundle.obfuscation();
		ProximityConfig proximityConfig = bundle.proximity();

		Set<BlockPos> blockEntities = new HashSet<>();
		Set<BlockPos> proximityBlocks = new HashSet<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

		int[] blockStates = new int[4096];

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct, bundle)) {
			for (int sectionIndex = Math.max(0, bundle.minSectionIndex()); sectionIndex <= Math
					.min(chunk.getSectionCount() - 1, bundle.maxSectionIndex()); sectionIndex++) {
				try {
					ChunkSection chunkSection = chunk.getSection(sectionIndex);
					if (chunkSection == null || chunkSection.isEmpty()) {
						ByteBuf chunkSectionBuffer = chunk.getSectionBuffer(sectionIndex);
						if (chunkSectionBuffer != null) {
							chunk.outputBuffer.writeBytes(chunkSectionBuffer);
						} else {
							chunk.outputBuffer.writeShort(0);
							chunk.outputBuffer.writeByte(0);
							ByteBufUtil.writeVarInt(chunk.outputBuffer, 0);
							ByteBufUtil.writeVarInt(chunk.outputBuffer, 0);
							chunk.outputBuffer.writeBytes(chunkSection.suffix);
						}
						continue;
					}

					int blockCount = chunkSection.blockCount;
					Palette.Builder builder = BlockPaletteAccessor.builder(chunkSection.blockPalette);

					final int baseY = heightAccessor.getMinBuildHeight() + (sectionIndex << 4);
					for (int index = 0; index < 4096; index++) {
						int blockState = chunkSection.getBlockState(index);

						int y = baseY + (index >> 8 & 15);
						if (bundle.shouldObfuscate(y)) {

							int obfuscateBits = blockFlags.flags(blockState, y);
							if (!BlockFlags.isEmpty(obfuscateBits)) {

								int x = baseX + (index & 15);
								int z = baseZ + (index >> 4 & 15);
								boolean obfuscated = true;

								if (BlockFlags.isObfuscateBitSet(obfuscateBits)
										&& shouldObfuscate(task, chunk, x, y, z)
										&& obfuscationConfig.shouldObfuscate(y)) {
									blockState = bundle.nextRandomObfuscationBlock(y);
								} else if (BlockFlags.isProximityBitSet(obfuscateBits)
										&& proximityConfig.shouldObfuscate(y)) {
									proximityBlocks.add(new BlockPos(x, y, z));

									if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
										blockState = getBlockStateBelow(blockFlags, chunk, x, y, z);
									} else {
										blockState = bundle.nextRandomProximityBlock(y);
									}
								} else {
									obfuscated = false;
								}

								if (obfuscated) {
									if (BlockFlags.isBlockEntityBitSet(obfuscateBits)) {
										blockEntities.add(new BlockPos(x, y, z));
									}
									if (NmsInstance.isAir(blockState)) {
										blockCount--;
									}
								}
							}
						}

						builder.add(blockState);
						blockStates[index] = blockState;
					}

					// create palette for section
					Palette blockPalette = builder.build();
					int bitsPerBlock = blockPalette.bitsPerEntry();

					// make sure buffer has enough space
					int packetSize = 3 + blockPalette.packetSize()
							+ BlockBitStorage.packetSize(bitsPerBlock)
							+ chunkSection.suffix.readableBytes();
					chunk.outputBuffer.ensureWritable(packetSize);

					// write section once
					if (ChunkCapabilities.hasBlockCount()) {
						chunk.outputBuffer.writeShort(blockCount);
					}

					chunk.outputBuffer.writeByte(bitsPerBlock);
					blockPalette.write(chunk.outputBuffer);

					BitStorage.Writer blockWriter = BlockBitStorage.write(chunk.outputBuffer, blockPalette);
					if (blockPalette.bitsPerEntry() > 0) {
						for (int i = 0; i < 4096; i++) {
							blockWriter.write(blockStates[i]);
						}
					}

					chunk.outputBuffer.writeBytes(chunkSection.suffix);
				} catch (Exception e) {
					throw new RuntimeException("error in section: " + sectionIndex, e);
				}
			}

			long duration = System.currentTimeMillis() - time;
			int count = COUNT.incrementAndGet();
			if (count > 0) {
				double totalTime = (double) TIME.addAndGet(duration);
				if (count % 100 == 0) {
					Bukkit.broadcastMessage("ms/chunk: " + (totalTime / count));
				}
			}

			task.complete(chunk.finalizeOutput(), blockEntities, proximityBlocks);
		} catch (Exception e) {
			task.completeExceptionally(e);
		}
	}

	// returns first block below given position that wouldn't be obfuscated in any
	// way at given position
	private int getBlockStateBelow(BlockFlags blockFlags, Chunk chunk, int x, int y, int z) {
		for (int targetY = y - 1; targetY > chunk.getHeightAccessor().getMinBuildHeight(); targetY--) {
			int blockData = chunk.getBlockState(x, targetY, z);
			if (blockData != -1 && BlockFlags.isEmpty(blockFlags.flags(blockData, y))) {
				return blockData;
			}
		}
		return 0;
	}

	private boolean shouldObfuscate(ObfuscationTask task, Chunk chunk, int x, int y, int z) {
		return isAdjacentBlockOccluding(task, chunk, x, y + 1, z)
				&& isAdjacentBlockOccluding(task, chunk, x, y - 1, z)
				&& isAdjacentBlockOccluding(task, chunk, x + 1, y, z)
				&& isAdjacentBlockOccluding(task, chunk, x - 1, y, z)
				&& isAdjacentBlockOccluding(task, chunk, x, y, z + 1)
				&& isAdjacentBlockOccluding(task, chunk, x, y, z - 1);
	}

	private boolean isAdjacentBlockOccluding(ObfuscationTask task, Chunk chunk, int x, int y, int z) {
		if (y >= chunk.getHeightAccessor().getMaxBuildHeight() || y < chunk.getHeightAccessor().getMinBuildHeight()) {
			return false;
		}

		int blockId = chunk.getBlockState(x, y, z);
		if (blockId == -1) {
			blockId = task.getBlockState(x, y, z);
		}

		return blockId >= 0 && NmsInstance.isOccluding(blockId);
	}
}
