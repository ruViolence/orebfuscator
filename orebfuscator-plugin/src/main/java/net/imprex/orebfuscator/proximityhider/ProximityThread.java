package net.imprex.orebfuscator.proximityhider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.player.PlayerData;
import net.imprex.orebfuscator.player.PlayerDataStorage;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.MathUtil;

public class ProximityThread extends Thread {

	private static final AtomicInteger NEXT_ID = new AtomicInteger();

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	private final ProximityQueue proximityQueue;
	private final PlayerDataStorage playerDataStorage;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public ProximityThread(ProximityHider proximityHider, Orebfuscator orebfuscator) {
		super(Orebfuscator.THREAD_GROUP, "ofc-proximity-hider-" + NEXT_ID.getAndIncrement());
		this.playerDataStorage = orebfuscator.getPlayerDataStorage();
		this.proximityQueue = proximityHider.getQueue();
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	@Override
	public void run() {
		while (this.running.get()) {
			try {
				Player player = this.proximityQueue.poll();
				try {
					if (player == null || !player.isOnline()) {
						continue;
					}

					Location location = player.getLocation();
					World world = location.getWorld();

					ProximityConfig proximityConfig = this.config.proximity(world);
					PlayerData playerData = this.playerDataStorage.get(player);
					if (playerData == null || proximityConfig == null || !proximityConfig.isEnabled()) {
						continue;
					}

					int distance = proximityConfig.distance();
					int distanceSquared = distance * distance;

					List<BlockPos> updateBlocks = new ArrayList<>();
					Location eyeLocation = player.getEyeLocation();

					int minChunkX = (location.getBlockX() - distance) >> 4;
					int maxChunkX = (location.getBlockX() + distance) >> 4;
					int minChunkZ = (location.getBlockZ() - distance) >> 4;
					int maxChunkZ = (location.getBlockZ() + distance) >> 4;

					for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
						for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
							Set<BlockPos> blocks = playerData.getProximityBlocks(new ChunkPosition(world, chunkX, chunkZ));
							if (blocks == null || blocks.isEmpty()) {
								continue;
							}

							for (Iterator<BlockPos> iterator = blocks.iterator(); iterator.hasNext(); ) {
								BlockPos blockCoords = iterator.next();
								Location blockLocation = new Location(world, blockCoords.x, blockCoords.y, blockCoords.z);

								if (location.distanceSquared(blockLocation) < distanceSquared) {
									if (!proximityConfig.useFastGazeCheck() || MathUtil.doFastCheck(blockLocation, eyeLocation, world)) {
										iterator.remove();
										updateBlocks.add(blockCoords);
									}
								}
							}
						}
					}

					Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
						if (player.isOnline()) {
							for (BlockPos blockCoords : updateBlocks) {
								NmsInstance.sendBlockChange(player, blockCoords);
							}
						}
					});
				} finally {
					this.proximityQueue.unlock(player);
				}
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		this.running.set(false);
		this.interrupt();
	}
}