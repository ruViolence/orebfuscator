package net.imprex.orebfuscator.player;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.proximityhider.ProximityHider;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class PlayerDataStorage implements Listener {

	private final Map<Player, PlayerData> playerData = new WeakHashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

	private final ProximityHider proximityHider;

	public PlayerDataStorage(Orebfuscator orebfuscator) {
		this.proximityHider = orebfuscator.getProximityHider();
		Bukkit.getPluginManager().registerEvents(this, orebfuscator);
	}

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		this.lock.writeLock().lock();
		try {
			this.playerData.put(event.getPlayer(), new PlayerData());
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.lock.writeLock().lock();
		try {
			this.playerData.remove(event.getPlayer());
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public PlayerData get(Player player) {
		this.lock.readLock().lock();
		try {
			return this.playerData.get(player);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	private void getGuarded(Player player, Consumer<PlayerData> guard) {
		PlayerData data = this.get(player);

		if (data != null) {
			synchronized (data) {
				guard.accept(data);
			}
		}
	}

	private <T> T getGuarded(Player player, Function<PlayerData, T> guard, T defaultValue) {
		PlayerData data = this.get(player);

		if (data != null) {
			synchronized (data) {
				return guard.apply(data);
			}
		}

		return defaultValue;
	}

	/**
	 * @return true if should be cancelled
	 */
	public boolean processChunk(Player player, ChunkPosition position) {
		return this.getGuarded(player, data -> data.processChunk(position), false);
	}

	/**
	 * @return true if should be cancelled
	 */
	public boolean preSendChunk(Player player, ChunkPosition position) {
		return this.getGuarded(player, data -> data.preSendChunk(position), false);
	}

	public void postSendChunk(Player player, ChunkPosition position, Set<BlockPos> proximityBlocks) {
		this.getGuarded(player, data -> data.postSendChunk(position, 
				this.proximityHider.isInProximityWorld(player) ? proximityBlocks : null));
	}

	public void unloadChunk(Player player, ChunkPosition position) {
		this.getGuarded(player, data -> data.unloadChunk(position));
	}
}
