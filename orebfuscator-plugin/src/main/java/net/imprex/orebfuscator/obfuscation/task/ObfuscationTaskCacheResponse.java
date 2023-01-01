package net.imprex.orebfuscator.obfuscation.task;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationTaskCacheResponse extends ObfuscationTask {

	private final ObfuscationTaskCacheRequest parent;
	private final ObfuscationResult result;

	public ObfuscationTaskCacheResponse(ObfuscationTaskCacheRequest task, ObfuscationResult result) {
		super(task);
		
		this.parent = task;
		this.result = result;
	}

	@Override
	public ObfuscationResult run(Orebfuscator orebfuscator, ObfuscationProcessor processor) throws Exception {
		if (this.parent.isValid(this.result)) {
			return this.result;
		}

		ChunkPosition position = this.request.getPosition();
		ObfuscationCache cache = orebfuscator.getObfuscationCache();

		ObfuscationResult result = processor.process(this.parent);
		cache.put(position, result);
		return result;
	}
}
