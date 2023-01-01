package net.imprex.orebfuscator.obfuscation;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.bukkit.Bukkit;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.AdvancedConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.obfuscation.task.ObfuscationTask;

class ObfuscationTaskDispatcher implements Runnable {

	private final OrebfuscatorConfig config;
	private final ObfuscationProcessor processor;

	private final Queue<ObfuscationRequest> requests = new ConcurrentLinkedQueue<>();
	private final Queue<ObfuscationTask> tasks = new ConcurrentLinkedQueue<>();

	private final long availableNanosPerTick;
	private final ObfuscationTaskWorker[] worker;

	public ObfuscationTaskDispatcher(Orebfuscator orebfuscator, ObfuscationProcessor processor) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.processor = processor;

		AdvancedConfig config = this.config.advanced();
		this.availableNanosPerTick = TimeUnit.MILLISECONDS.toNanos(config.maxMillisecondsPerTick());

		this.worker = new ObfuscationTaskWorker[config.obfuscationWorkerThreads()];
		for (int i = 0; i < this.worker.length; i++) {
			this.worker[i] = new ObfuscationTaskWorker(orebfuscator, this, this.processor);
		}

		Bukkit.getScheduler().runTaskTimer(orebfuscator, this, 0, 1);
	}

	public void offerRequest(ObfuscationRequest request) {
		this.requests.offer(request);
	}

	public void offerObfuscationTask(ObfuscationTask task) {
		this.tasks.offer(task);
	}

	public ObfuscationTask retrieveTask() throws InterruptedException {
		ObfuscationTask task;

		while ((task = this.tasks.poll()) == null) {
			LockSupport.park(this);
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}

		return task;
	}

	@Override
	public void run() {
		final long time = System.nanoTime();

		ObfuscationRequest request = null;
		while (System.nanoTime() - time < this.availableNanosPerTick && (request = this.requests.poll()) != null) {
			this.tasks.offer(ObfuscationTask.fromRequest(this.config, request));
		}

		// try to wake up enough threads for all pending tasks
		for (int i = 0; i < this.tasks.size() && i < this.worker.length; i++) {
			this.worker[i].unpark();
		}
	}

	public void shutdown() {
		for (ObfuscationTaskWorker worker : this.worker) {
			worker.shutdown();
		}
	}
}
