package net.imprex.orebfuscator;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.BlockStateHolder;
import net.imprex.orebfuscator.nms.NmsManager;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class NmsInstance {

	private static NmsManager instance;

	public static void initialize(Config config) {
		if (NmsInstance.instance != null) {
			throw new IllegalStateException("NMS adapter is already initialized!");
		}

		String nmsVersion = MinecraftVersion.getNmsVersion();
		OFCLogger.debug("Searching NMS adapter for server version \"" + nmsVersion + "\"!");

		try {
			String className = "net.imprex.orebfuscator.nms." + nmsVersion + ".NmsManager";
			Class<? extends NmsManager> nmsManager = Class.forName(className).asSubclass(NmsManager.class);
			Constructor<? extends NmsManager> constructor = nmsManager.getConstructor(Config.class);
			NmsInstance.instance = constructor.newInstance(config);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Server version \"" + nmsVersion + "\" is currently not supported!", e);
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize NMS adapter", e);
		}

		OFCLogger.debug("NMS adapter for server version \"" + nmsVersion + "\" found!");
	}

	public static AbstractRegionFileCache<?> getRegionFileCache() {
		return instance.getRegionFileCache();
	}

	public static int getBitsPerBlock() {
		return instance.getBitsPerBlock();
	}

	public static int getTotalBlockCount() {
		return instance.getTotalBlockCount();
	}

	public static Optional<Material> getMaterialByName(String name) {
		return instance.getMaterialByName(name);
	}

	public static Optional<String> getNameByMaterial(Material material) {
		return instance.getNameByMaterial(material);
	}

	public static Set<Integer> getBlockIds(Material material) {
		return instance.getBlockIds(material);
	}

	public static Optional<Integer> getFirstBlockId(Material material) {
		Iterator<Integer> blockIds = getBlockIds(material).iterator();
		return Optional.ofNullable(blockIds.hasNext() ? blockIds.next() : null);
	}

	public static boolean isHoe(Material material) {
		return instance.isHoe(material);
	}

	public static boolean isAir(int blockId) {
		return instance.isAir(blockId);
	}

	public static boolean isOccluding(int blockId) {
		return instance.isOccluding(blockId);
	}

	public static boolean isBlockEntity(int blockId) {
		return instance.isBlockEntity(blockId);
	}

	public static ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		return instance.getReadOnlyChunk(world, chunkX, chunkZ);
	}

	public static BlockStateHolder getBlockState(World world, Block block) {
		return instance.getBlockState(world, block.getX(), block.getY(), block.getZ());
	}

	public static BlockStateHolder getBlockState(World world, int x, int y, int z) {
		return instance.getBlockState(world, x, y, z);
	}

	public static boolean sendBlockChange(Player player, BlockPos blockCoords) {
		return instance.sendBlockChange(player, blockCoords.x, blockCoords.y, blockCoords.z);
	}

	public static void close() {
		if (instance != null) {
			instance.close();
			instance = null;
		}
	}
}