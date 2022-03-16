package net.imprex.orebfuscator.player;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkData {

	private final ChunkPosition position;
	private final Queue<ChunkFrame> pendingFrames = new LinkedList<>();

	private Set<BlockPos> proximityBlocks;
	private Runnable unload;

	public ChunkData(ChunkPosition position) {
		this.position = position;
	}

	private ChunkFrame getFrame(long packetId) {
		for (ChunkFrame frame : this.pendingFrames) {
			if (frame.getPacketId() == packetId) {
				return frame;
			}
		}
		return null;
	}

	void unloadFrame(ChunkFrame frame, boolean canDiscard) {
		this.pendingFrames.remove(frame);
		if (this.pendingFrames.isEmpty()) {
			this.unload.run();
		}

		if (canDiscard) {
			frame.discardFrame();
		}
	}

	void finalizeFrame(Set<BlockPos> proximityBlocks) {
		if (this.pendingFrames.size() == 1) {
			this.proximityBlocks = proximityBlocks;
		}
	}

	public ChunkPosition getPosition() {
		return position;
	}

	public void loadChunk(long packetId, Runnable discard) {
		this.pendingFrames.add(new ChunkFrame(this, packetId, discard));
	}

	public boolean preSendChunk(long packetId) {
		return getFrame(packetId).preSendChunk();
	}

	public void postSendChunk(long packetId, Set<BlockPos> proximityBlocks) {
		getFrame(packetId).postSendChunk(proximityBlocks);
	}

	public void unloadChunk(Runnable unload) {
		this.unload = unload;

		if (!this.pendingFrames.isEmpty()) {
			this.pendingFrames.peek().unloadChunk();
		}
	}

	public Set<BlockPos> getProximityBlocks() {
		return proximityBlocks;
	}
}
