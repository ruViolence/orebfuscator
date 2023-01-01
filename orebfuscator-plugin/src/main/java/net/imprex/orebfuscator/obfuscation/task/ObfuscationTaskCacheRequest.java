package net.imprex.orebfuscator.obfuscation.task;

import java.util.Arrays;
import java.util.Set;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationTaskCacheRequest extends ObfuscationTask {

	private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();

	private final byte[] hash = new byte[HASH_FUNCTION.bits() / Byte.SIZE];

	ObfuscationTaskCacheRequest(ObfuscationRequest request) {
		super(request);
	}

	@Override
	public ObfuscationResult run(Orebfuscator orebfuscator, ObfuscationProcessor processor) throws Exception {
		OrebfuscatorConfig config = orebfuscator.getOrebfuscatorConfig();
		this.computeHash(config);

		ObfuscationCache cache = orebfuscator.getObfuscationCache();
		CacheConfig cacheConfig = config.cache();

		ChunkPosition position = this.request.getPosition();
		ObfuscationResult result = cache.get(position);
		if (this.isValid(result)) {
			return result;
		}

		if (cacheConfig.enableDiskCache()) {
			cache.request(this);
			return null;
		} else {
			result = processor.process(this);
			cache.put(position, result);
			return result;
		}
	}

	@Override
	public ObfuscationResult createResult(byte[] data, Set<BlockPos> blockEntities, Set<BlockPos> proximityBlocks) {
		return new ObfuscationResult(this.request.getPosition(), this.hash, data, blockEntities, proximityBlocks);
	}

	boolean isValid(ObfuscationResult result) {
		return result != null && Arrays.equals(result.getHash(), this.hash);
	}

	private void computeHash(OrebfuscatorConfig config) {
		ChunkStruct chunkStruct = this.request.getChunkStruct();
		HeightAccessor heightAccessor = HeightAccessor.get(chunkStruct.world);

		Hasher hasher = HASH_FUNCTION.newHasher()
				.putBytes(config.systemHash())
				.putBytes(chunkStruct.data);

		for (ChunkDirection neighborDirection : ChunkDirection.values()) {

			ReadOnlyChunk neighbor = this.getNeighoringChunk(neighborDirection);
			ChunkDirection incrementalDirection = neighborDirection.rotate();

			int x = neighborDirection.getHashStartX();
			int z = neighborDirection.getHashStartX();

			for (int offset = 0; offset < 16; offset++) {

				int height = neighbor.getHeight(x, z);
				hasher.putInt(height);

				for (int y = heightAccessor.getMinBuildHeight(); y <= height && y < heightAccessor.getMaxBuildHeight(); y++) {
					int blockState = neighbor.getBlockState(x, y, z);
					hasher.putInt(blockState);
				}

				x += incrementalDirection.getOffsetX();
				z += incrementalDirection.getOffsetZ();
			}
		}

		hasher.hash().writeBytesTo(hash, 0, hash.length);
	}
}
