package net.imprex.orebfuscator.obfuscation.task;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;

public class ObfuscationTaskObfuscate extends ObfuscationTask {

	ObfuscationTaskObfuscate(ObfuscationRequest request) {
		super(request);
	}

	@Override
	public ObfuscationResult run(Orebfuscator orebfuscator, ObfuscationProcessor processor) throws Exception {
		return processor.process(this);
	}
}
