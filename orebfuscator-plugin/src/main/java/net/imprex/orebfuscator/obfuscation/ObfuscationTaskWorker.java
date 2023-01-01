package net.imprex.orebfuscator.obfuscation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.obfuscation.task.ObfuscationTask;

class ObfuscationTaskWorker implements Runnable {

	private static final AtomicInteger WORKER_ID = new AtomicInteger();

	private final Orebfuscator orebfuscator;
	private final ObfuscationTaskDispatcher dispatcher;
	private final ObfuscationProcessor processor;

	private final Thread thread;
	private volatile boolean running = true;

	public ObfuscationTaskWorker(Orebfuscator orebfuscator, ObfuscationTaskDispatcher dispatcher, ObfuscationProcessor processor) {
		this.orebfuscator = orebfuscator;
		this.dispatcher = dispatcher;
		this.processor = processor;

		this.thread = new Thread(Orebfuscator.THREAD_GROUP, this, "ofc-task-worker-" + WORKER_ID.getAndIncrement());
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public boolean unpark() {
		if (LockSupport.getBlocker(this.thread) == this.dispatcher) {
			LockSupport.unpark(this.thread);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		while (this.running) {
			try {
				ObfuscationTask task = this.dispatcher.retrieveTask();
				try {
					ObfuscationResult result = task.run(this.orebfuscator, this.processor);
					if (result != null) {
						task.complete(result);
					}
				} catch (Exception e) {
					task.completeExceptionally(e);
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public void shutdown() {
		this.running = false;
		this.thread.interrupt();
	}
}
