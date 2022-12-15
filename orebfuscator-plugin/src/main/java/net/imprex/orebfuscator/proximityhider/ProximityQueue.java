package net.imprex.orebfuscator.proximityhider;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.entity.Player;

public class ProximityQueue {

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private final Queue<Player> queue = new LinkedList<>();
	private final Set<Player> isQueued = new HashSet<>();

	public void offer(Player player) {
		Objects.requireNonNull(player);

		lock.lock();
		try {
			if (this.isQueued.add(player)) {
				boolean empty = this.queue.isEmpty();
				this.queue.offer(player);
				if (empty) {
					this.notEmpty.signal();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public Player startProcessing() throws InterruptedException {
		lock.lock();
		try {
			if (this.queue.isEmpty()) {
				this.notEmpty.await();
			}
			return this.queue.poll();
		} finally {
			lock.unlock();
		}
	}

	public void processingDone(Player player) {
		lock.lock();
		try {
			this.isQueued.remove(player);
		} finally {
			lock.unlock();
		}
	}

	public void remove(Player player) {
		lock.lock();
		try {
			if (this.isQueued.remove(player)) {
				this.queue.remove(player);
			}
		} finally {
			lock.unlock();
		}
	}

	public void clear() {
		lock.lock();
		try {
			this.isQueued.clear();
			this.queue.clear();
		} finally {
			lock.unlock();
		}
	}
}
