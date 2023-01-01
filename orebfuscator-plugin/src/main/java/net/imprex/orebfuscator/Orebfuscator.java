package net.imprex.orebfuscator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.imprex.orebfuscator.api.OrebfuscatorService;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import net.imprex.orebfuscator.proximityhider.ProximityHider;
import net.imprex.orebfuscator.proximityhider.ProximityListener;
import net.imprex.orebfuscator.proximityhider.ProximityPacketListener;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.OFCLogger;

public class Orebfuscator extends JavaPlugin implements Listener {

	public static final ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

	private final Thread mainThread = Thread.currentThread();

	private OrebfuscatorConfig config;
	private UpdateSystem updateSystem;
	private ObfuscationCache obfuscationCache;
	private ObfuscationSystem obfuscationSystem;
	private ProximityHider proximityHider;
	private ProximityPacketListener proximityPacketListener;

	@Override
	public void onEnable() {
		try {
			// Check if protocolLib is enabled
			if (this.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
				OFCLogger.log(Level.SEVERE, "ProtocolLib is not found! Plugin cannot be enabled.");
				return;
			}

			// Load configurations
			this.config = new OrebfuscatorConfig(this);

			// Check if HeightAccessor can be loaded
			HeightAccessor.thisMethodIsUsedToInitializeStaticFieldsEarly();

			// Initialize metrics
			new MetricsSystem(this);

			// initialize update system and check for updates
			this.updateSystem = new UpdateSystem(this);

			// Load chunk cache
			this.obfuscationCache = new ObfuscationCache(this);

			// Load obfuscater
			this.obfuscationSystem = new ObfuscationSystem(this);

			// Load proximity hider
			this.proximityHider = new ProximityHider(this);
			if (this.config.proximityEnabled()) {
				this.proximityHider.start();

				this.proximityPacketListener = new ProximityPacketListener(this);

				this.getServer().getPluginManager().registerEvents(new ProximityListener(this), this);
			}

			// Load packet listener
			this.obfuscationSystem.registerChunkListener();

			// Store formatted config
			this.config.store();
			
			// initialize service
			Bukkit.getServicesManager().register(
					OrebfuscatorService.class,
					new DefaultOrebfuscatorService(this),
					this, ServicePriority.Normal);
			
			getServer().getPluginManager().registerEvents(this, this);
		} catch (Exception e) {
			OFCLogger.error("An error occurred while enabling plugin", e);

			this.getServer().getPluginManager().registerEvent(PluginEnableEvent.class, this, EventPriority.NORMAL,
					this::onEnableFailed, this);
		}
	}

	@Override
	public void onDisable() {
		if (this.obfuscationCache != null) {
			this.obfuscationCache.close();
		}

		if (this.obfuscationSystem != null) {
			this.obfuscationSystem.shutdown();
		}

		if (this.config.proximityEnabled() && this.proximityPacketListener != null && this.proximityHider != null) {
			this.proximityPacketListener.unregister();
			this.proximityHider.close();
		}

		this.getServer().getScheduler().cancelTasks(this);

		NmsInstance.close();
		this.config = null;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getHand() == EquipmentSlot.HAND && event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			Block block = event.getClickedBlock();
			int chunkX = block.getX() >> 4;
			int chunkZ = block.getZ() >> 4;

			List<Location> list = new ArrayList<>();

			for (ChunkDirection direction : ChunkDirection.values()) {
				ChunkDirection incrementDirection = direction.rotate();

				ReadOnlyChunk readChunk = NmsInstance.getReadOnlyChunk(block.getWorld(),
						chunkX + direction.getOffsetX(),
						chunkZ + direction.getOffsetZ());

				Location location = block.getLocation().clone();
				location.setX((chunkX + direction.getOffsetX()) << 4);
				location.setZ((chunkZ + direction.getOffsetZ()) << 4);

				int x, z, height;

				for (int offset = 0; offset < 16; offset++) {
					x = direction.getHashStartX() + offset * incrementDirection.getOffsetX();
					z = direction.getHashStartZ() + offset * incrementDirection.getOffsetZ();

					height = readChunk.getHeight(x, z);

					Location locationHeight = location.clone().add(x, 0, z);
					locationHeight.setY(height);
					list.add(locationHeight);
				}
			}

			final AtomicReference<BukkitTask> reference = new AtomicReference<BukkitTask>();
			reference.set(Bukkit.getScheduler().runTaskTimer(this, new Runnable() {

				private int index = 0;
				private Material[] material = new Material[list.size()];

				@Override
				public void run() {
					Location location = list.get(index % list.size());
					Block block = location.clone().add(0, 1, 0).getBlock();

					if (index < list.size()) {
						material[index] = block.getType();
						block.setType(Material.RED_STAINED_GLASS);
					} else if (index < list.size() * 2) {
						block.setType(material[index % material.length]);
					} else {
						reference.get().cancel();
					}

					index++;
				}
			}, 5L, 5L));
		}
	}

	public void onEnableFailed(Listener listener, Event event) {
		PluginEnableEvent enableEvent = (PluginEnableEvent) event;

		if (enableEvent.getPlugin() == this) {
			HandlerList.unregisterAll(listener);
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public boolean isGameThread() {
		return Thread.currentThread() == this.mainThread;
	}

	public OrebfuscatorConfig getOrebfuscatorConfig() {
		return this.config;
	}

	public UpdateSystem getUpdateSystem() {
		return updateSystem;
	}

	public ObfuscationCache getObfuscationCache() {
		return this.obfuscationCache;
	}

	public ObfuscationSystem getObfuscationSystem() {
		return obfuscationSystem;
	}

	public ProximityHider getProximityHider() {
		return this.proximityHider;
	}

	public ProximityPacketListener getProximityPacketListener() {
		return this.proximityPacketListener;
	}
}