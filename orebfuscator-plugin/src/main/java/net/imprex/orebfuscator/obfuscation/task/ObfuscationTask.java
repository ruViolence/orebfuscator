package net.imprex.orebfuscator.obfuscation.task;

import java.util.Set;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;

public abstract class ObfuscationTask {

	public static ObfuscationTask fromRequest(OrebfuscatorConfig config, ObfuscationRequest request) {
		ObfuscationTask task;

		if (config.cache().enabled()) {
			task = new ObfuscationTaskCacheRequest(request);
		} else {
			task = new ObfuscationTaskObfuscate(request);
		}

		ChunkPosition position = request.getPosition();
		for (ChunkDirection direction : ChunkDirection.values()) {
			int chunkX = position.getX() + direction.getOffsetX();
			int chunkZ = position.getZ() + direction.getOffsetZ();
			ReadOnlyChunk chunk = NmsInstance.getReadOnlyChunk(position.getWorld(), chunkX, chunkZ);
			task.neighboringChunks[direction.ordinal()] = chunk;
		}

		return task;
	}

	protected final ObfuscationRequest request;

	private final ReadOnlyChunk[] neighboringChunks;

	protected ObfuscationTask(ObfuscationRequest request) {
		this.request = request;
		this.neighboringChunks = new ReadOnlyChunk[4];
	}

	protected ObfuscationTask(ObfuscationTask task) {
		this.request = task.request;
		this.neighboringChunks = task.neighboringChunks;
	}

	public ChunkPosition getPosition() {
		return this.request.getPosition();
	}

	public ChunkStruct getChunkStruct() {
		return this.request.getChunkStruct();
	}

	public void complete(ObfuscationResult result) {
		this.request.getFuture().complete(result);
	}

	public void completeExceptionally(Throwable throwable) {
		this.request.getFuture().completeExceptionally(throwable);
	}

	protected final ReadOnlyChunk getNeighoringChunk(ChunkDirection direction) {
		return this.neighboringChunks[direction.ordinal()];
	}

	public int getBlockState(int x, int y, int z) {
		ChunkDirection direction = ChunkDirection.fromPosition(request.getPosition(), x, z);
		return this.getNeighoringChunk(direction).getBlockState(x, y, z);
	}

	public abstract ObfuscationResult run(Orebfuscator orebfuscator, ObfuscationProcessor processor) throws Exception;

	public ObfuscationResult createResult(byte[] data, Set<BlockPos> blockEntities, Set<BlockPos> proximityBlocks) {
		return new ObfuscationResult(this.request.getPosition(), null, data, blockEntities, proximityBlocks);
	}
}
