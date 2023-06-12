package net.imprex.orebfuscator.proximityhider;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;

import net.imprex.orebfuscator.Orebfuscator;

public class ProximityWorkerThread extends Thread {

	private static final AtomicInteger NEXT_ID = new AtomicInteger();

	private final ProximityDirectorThread directorThread;
	private final ProximityWorker worker;

	public ProximityWorkerThread(ProximityDirectorThread directorThread, ProximityWorker worker) {
		super(Orebfuscator.THREAD_GROUP, "ofc-proximity-worker-" + NEXT_ID.getAndIncrement());
		this.setDaemon(true);

		this.directorThread = directorThread;
		this.worker = worker;
	}

	@Override
	public void run() {
		while (this.directorThread.isRunning()) {
			try {
				List<Player> bucket = this.directorThread.nextBucket();

				for (Player player : bucket) {
					this.worker.process(player);
				}

				this.directorThread.finishBucketProcessing();
			} catch (InterruptedException e) {
				continue;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
