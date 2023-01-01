package net.imprex.orebfuscator.obfuscation;

import java.util.Set;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationTask {

	private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();

	public static ObfuscationTask fromRequest(ObfuscationRequest request) {
		ObfuscationTask task = new ObfuscationTask(request);

		ChunkPosition position = request.getPosition();
		for (ChunkDirection direction : ChunkDirection.values()) {
			int chunkX = position.getX() + direction.getOffsetX();
			int chunkZ = position.getZ() + direction.getOffsetZ();
			ReadOnlyChunk chunk = NmsInstance.getReadOnlyChunk(position.getWorld(), chunkX, chunkZ);
			task.neighboringChunks[direction.ordinal()] = chunk;
		}

		return task;
	}

	private final ObfuscationRequest request;

	private final byte[] hash = new byte[HASH_FUNCTION.bits() / Byte.SIZE];
	private final ReadOnlyChunk[] neighboringChunks = new ReadOnlyChunk[4];

	private ObfuscationTask(ObfuscationRequest request) {
		this.request = request;
	}

	public ChunkPosition getPosition() {
		return this.request.getPosition();
	}

	public ChunkStruct getChunkStruct() {
		return this.request.getChunkStruct();
	}

	public byte[] getHash() {
		return hash;
	}

	public void complete(byte[] data, Set<BlockPos> blockEntities, Set<BlockPos> proximityBlocks) {
		this.request.complete(this.request.createResult(data, blockEntities, proximityBlocks));
	}

	public void completeExceptionally(Throwable throwable) {
		this.request.completeExceptionally(throwable);
	}

	private ReadOnlyChunk getNeighoringChunk(ChunkDirection direction) {
		return this.neighboringChunks[direction.ordinal()];
	}

	public void computeHash(OrebfuscatorConfig config) {
		ChunkStruct chunkStruct = this.getChunkStruct();
		HeightAccessor heightAccessor = HeightAccessor.get(chunkStruct.world);

		Hasher hasher = HASH_FUNCTION.newHasher()
				.putBytes(config.systemHash())
				.putBytes(chunkStruct.data);

		for (ChunkDirection direction : ChunkDirection.values()) {

			ReadOnlyChunk chunk = this.getNeighoringChunk(direction);
			ChunkDirection incrementDirection = direction.rotate();

			int x, z, height;

			for (int offset = 0; offset < 16; offset++) {
				x = direction.getHashStartX() + offset * incrementDirection.getOffsetX();
				z = direction.getHashStartZ() + offset * incrementDirection.getOffsetZ();

				height = chunk.getHeight(x, z);
				hasher.putInt(height);

				for (int y = heightAccessor.getMinBuildHeight(); y <= height && y < heightAccessor.getMaxBuildHeight(); y++) {
					int blockState = chunk.getBlockState(x, y, z);
					hasher.putInt(blockState);
				}
			}
		}

		hasher.hash().writeBytesTo(hash, 0, hash.length);
	}

	public int getBlockState(int x, int y, int z) {
		ChunkDirection direction = ChunkDirection.fromPosition(request.getPosition(), x, z);
		return this.getNeighoringChunk(direction).getBlockState(x, y, z);
	}
}
