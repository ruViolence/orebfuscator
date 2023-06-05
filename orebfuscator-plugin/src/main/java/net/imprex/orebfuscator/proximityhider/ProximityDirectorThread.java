package net.imprex.orebfuscator.proximityhider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.Orebfuscator;

public class ProximityDirectorThread extends Thread {

	private final CyclicBarrier cyclicBarrier;
	private volatile boolean running = true;

	private final int workerCount;
	private final Queue<List<Player>> bucketQueue = new LinkedList<>();

	private final Lock bucketLock = new ReentrantLock();
	private final Condition nextBucket = bucketLock.newCondition();

	private final ProximityWorker worker;
	private final ProximityWorkerThread[] workerThreads;

	public ProximityDirectorThread(Orebfuscator orebfuscator) {
		super(Orebfuscator.THREAD_GROUP, "ofc-proximity-director");

		this.workerCount = 1;//orebfuscator.getOrebfuscatorConfig().advanced().proximityHiderThreads();
		this.cyclicBarrier = new CyclicBarrier(this.workerCount);

		this.worker = new ProximityWorker(orebfuscator);
		this.workerThreads = new ProximityWorkerThread[workerCount - 1];
	}

	@Override
	public void start() {
		super.start();

		for (int i = 0; i < workerCount - 1; i++) {
			this.workerThreads[i] = new ProximityWorkerThread(this, this.worker);
			this.workerThreads[i].start();
		}
	}

	public void close() {
		this.running = false;

		this.interrupt();
		for (int i = 0; i < workerCount - 1; i++) {
			this.workerThreads[i].interrupt();
		}
	}

	boolean isRunning() {
		return this.running;
	}

	List<Player> getBucket() throws InterruptedException {
		this.bucketLock.lock();
		try {
			this.nextBucket.await();
			return bucketQueue.poll();
		} finally {
			this.bucketLock.unlock();
		}
	}

	void awaitNextExecution() throws InterruptedException, BrokenBarrierException {
		this.cyclicBarrier.await();
	}

	@Override
	public void run() {
		while (this.isRunning()) {
			try {
				Collection<? extends Player> players = Bukkit.getOnlinePlayers();
				Iterator<? extends Player> iterator = players.iterator();

				int playerCount = players.size();
				int bucketCount = Math.min((int) Math.ceil((float) playerCount / 50f), this.workerCount);
				int bucketSize = (int) Math.ceil((float) playerCount / (float) bucketCount);

				List<Player> localBucket = null;
				
				this.bucketLock.lock();
				try {
					// refill buckets
					for (int index = 0; index < bucketCount; index++) {
						List<Player> bucket = new ArrayList<>();

						for (int size = 0; size < bucketSize && iterator.hasNext(); size++) {
							bucket.add(iterator.next());
						}

						this.bucketQueue.offer(bucket);
					}

					System.out.println("buckets: " + this.bucketQueue);

					// get own bucket
					localBucket = this.bucketQueue.poll();

					// wake up enough threads for the amount of created buckets
					for (int i = 0; i < bucketCount - 1; i++) {
						this.nextBucket.signal();
					}
				} finally {
					this.bucketLock.unlock();
				}

				// process local bucket
				if (localBucket != null) {
					for (Player player : localBucket) {
						this.worker.process(player);
					}
				}

				// wait for all threads to finish and reset barrier
				this.awaitNextExecution();
				this.cyclicBarrier.reset();

				// sleep till next execution
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				continue;
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
