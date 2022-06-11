package net.imprex.orebfuscator.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class PlayerData {

	private final Map<ChunkPosition, Boolean> shouldUnloadChunk = new HashMap<>();
	private final Map<ChunkPosition, Set<BlockPos>> proximityData = new HashMap<>();

	/**
	 * @return true if should be cancelled
	 */
	public boolean processChunk(ChunkPosition position) {
		if (this.shouldUnloadChunk.containsKey(position)) {
			return true;
		} else {
			this.shouldUnloadChunk.put(position, Boolean.FALSE);
			return false;
		}
	}

	/**
	 * @return true if should be cancelled
	 */
	public boolean preSendChunk(ChunkPosition position) {
		if (this.shouldUnloadChunk.get(position) == Boolean.TRUE) {
			this.shouldUnloadChunk.remove(position);
			return true;
		}
		return false;
	}

	public void postSendChunk(ChunkPosition position, Set<BlockPos> proximityBlocks) {
		this.shouldUnloadChunk.remove(position);
		this.proximityData.put(position, proximityBlocks);
	}

	public void unloadChunk(ChunkPosition position) {
		this.shouldUnloadChunk.computeIfPresent(position, (key, value) -> Boolean.TRUE);
		this.proximityData.remove(position);
	}

	public Set<BlockPos> getProximityBlocks(ChunkPosition position) {
		return this.proximityData.get(position);
	}
}
