package net.imprex.orebfuscator.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class PlayerData {

	private final Map<ChunkPosition, ChunkData> loadedChunks = new HashMap<>();

	/**
	 * Should be called when a chunk is loaded
	 * 
	 * @param position the chunk's position
	 */
	public void loadChunk(ChunkPosition position, long packetId, Runnable discard) {
		this.loadedChunks.computeIfAbsent(position, ChunkData::new).loadChunk(packetId, discard);
	}

	/**
	 * Should be called before a processed chunk is being sent
	 * 
	 * @param position the chunk's position
	 * @return true if the chunk should be send
	 */
	public boolean preSendChunk(ChunkPosition position, long packetId) {
		ChunkData chunk = this.loadedChunks.get(position);
		return chunk != null ? chunk.preSendChunk(packetId) : false;
	}

	/**
	 * Should be called after a processed chunk is sent
	 * 
	 * @param position        the chunk's position
	 * @param proximityBlocks set of proximity hidden blocks in chunk
	 */
	public void postSendChunk(ChunkPosition position, long packetId, Set<BlockPos> proximityBlocks) {
		ChunkData chunk = this.loadedChunks.get(position);
		if (chunk != null) {
			chunk.postSendChunk(packetId, proximityBlocks);
		}
	}

	/**
	 * Should be called when a chunk is unloaded
	 * 
	 * @param position the chunk's position
	 * @param unload   Runnable that should unload the chunk on call
	 * @return true if the chunk should be unloaded
	 */
	public void unloadChunk(ChunkPosition position, Runnable unload) {
		ChunkData chunk = this.loadedChunks.get(position);
		if (chunk != null) {
			chunk.unloadChunk(() -> {
				unload.run();
				this.loadedChunks.remove(position);
			});
		}
	}

	public Set<BlockPos> getProximityBlocks(ChunkPosition position) {
		ChunkData chunk = this.loadedChunks.get(position);
		return chunk != null ? chunk.getProximityBlocks() : null;
	}
}
