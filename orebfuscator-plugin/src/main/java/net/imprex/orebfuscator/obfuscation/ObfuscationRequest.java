package net.imprex.orebfuscator.obfuscation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationRequest {

	public static CompletionStage<ObfuscationResult> fromChunk(ObfuscationTaskDispatcher dispatcher, ChunkStruct struct) {
		ObfuscationRequest request = new ObfuscationRequest(struct);
		dispatcher.offerRequest(request);
		return request.future;
	}

	private final CompletableFuture<ObfuscationResult> future = new CompletableFuture<>();

	private final ChunkPosition position;
	private final ChunkStruct chunkStruct;

	private ObfuscationRequest(ChunkStruct chunkStruct) {
		this.position = new ChunkPosition(chunkStruct.world, chunkStruct.chunkX, chunkStruct.chunkZ);
		this.chunkStruct = chunkStruct;
	}

	public ChunkPosition getPosition() {
		return position;
	}

	public ChunkStruct getChunkStruct() {
		return chunkStruct;
	}

	public CompletableFuture<ObfuscationResult> getFuture() {
		return future;
	}
}
