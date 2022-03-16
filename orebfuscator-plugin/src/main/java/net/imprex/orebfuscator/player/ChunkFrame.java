package net.imprex.orebfuscator.player;

import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;

class ChunkFrame {

	private final ChunkData chunk;

	private final long packetId;
	private final Runnable discard;

	private ChunkFrameState state = ChunkFrameState.LOADED;

	public ChunkFrame(ChunkData chunk, long packetId, Runnable discard) {
		this.chunk = chunk;
		this.packetId = packetId;
		this.discard = discard;
	}

	public long getPacketId() {
		return packetId;
	}

	public void discardFrame() {
		if (this.discard != null) {
			this.discard.run();
		}
	}

	public boolean preSendChunk() {
		if (this.state == ChunkFrameState.LOADED) {
			this.state = ChunkFrameState.SENDING;
			return true;
		} else if (this.state == ChunkFrameState.UNLOADED) {
			this.chunk.unloadFrame(this, true);
			return false;
		} else {
			throw new IllegalStateException(this.state.toString());
		}
	}

	public void postSendChunk(Set<BlockPos> proximityBlocks) {
		if (this.state == ChunkFrameState.SENDING) {
			this.state = ChunkFrameState.SENT;
			this.chunk.finalizeFrame(proximityBlocks);
		} else if (this.state == ChunkFrameState.UNLOADED) {
			this.chunk.unloadFrame(this, false);
		} else {
			throw new IllegalStateException(this.state.toString());
		}
	}

	public void unloadChunk() {
		if (this.state == ChunkFrameState.LOADED) {
			this.chunk.unloadFrame(this, true);
		} else if (this.state == ChunkFrameState.SENT) {
			this.chunk.unloadFrame(this, false);
		} else if (this.state == ChunkFrameState.SENDING) {
			this.state = ChunkFrameState.UNLOADED;
		} else {
			throw new IllegalStateException(this.state.toString());
		}
	}

	public enum ChunkFrameState {

		LOADED, SENDING, SENT, UNLOADED;
	}
}