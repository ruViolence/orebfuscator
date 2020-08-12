package net.imprex.orebfuscator;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.BlockStateHolder;
import net.imprex.orebfuscator.nms.NmsManager;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.OFCLogger;

public class NmsInstance {

	public static final String SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

	private static NmsManager instance;

	public static void initialize(Config config) {
		if (NmsInstance.instance != null) {
			throw new IllegalStateException("NMS protocol version was already initialized!");
		}

		OFCLogger.log("Searching NMS protocol for server version \"" + SERVER_VERSION + "\"!");

		// hasSimpleVarBitBuffer >= 1.16
		// hasBlockCount >= 1.14
		// hasLight < 1.14
		// hasDirectPaletteZeroLength < 1.13

		switch (SERVER_VERSION) {
		case "v1_16_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_16_R2.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			ChunkCapabilities.hasSimpleVarBitBuffer();
			break;

		case "v1_16_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_16_R1.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			ChunkCapabilities.hasSimpleVarBitBuffer();
			break;

		case "v1_15_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_15_R1.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			break;

		case "v1_14_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_14_R1.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			break;

		case "v1_13_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R2.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			break;

		case "v1_13_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			break;

		case "v1_12_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_12_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_11_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_11_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_10_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_10_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_9_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_9_R2.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;
		}

		if (NmsInstance.instance != null) {
			OFCLogger.log("NMS protocol for server version \"" + SERVER_VERSION + "\" found!");
		} else {
			throw new RuntimeException("Server version \"" + SERVER_VERSION + "\" is currently not supported!");
		}
	}

	public static AbstractRegionFileCache<?> getRegionFileCache() {
		return instance.getRegionFileCache();
	}

	public static int getBitsPerBlock() {
		return instance.getBitsPerBlock();
	}

	public static int getMaterialSize() {
		return instance.getMaterialSize();
	}

	public static Optional<Material> getMaterialByName(String name) {
		return instance.getMaterialByName(name);
	}

	public static Set<Integer> getMaterialIds(Material material) {
		return instance.getMaterialIds(material);
	}

	public static int getCaveAirBlockId() {
		return instance.getCaveAirBlockId();
	}

	public static boolean isHoe(Material material) {
		return instance.isHoe(material);
	}

	public static boolean isAir(int blockId) {
		return instance.isAir(blockId);
	}

	public static boolean isTileEntity(int blockId) {
		return instance.isTileEntity(blockId);
	}

	public static boolean canApplyPhysics(Material material) {
		return instance.canApplyPhysics(material);
	}

	public static void updateBlockTileEntity(Player player, BlockCoords blockCoord) {
		instance.updateBlockTileEntity(player, blockCoord);
	}

	public static int getBlockLightLevel(World world, int x, int y, int z) {
		return instance.getBlockLightLevel(world, x, y, z);
	}

	public static BlockStateHolder getBlockState(World world, int x, int y, int z) {
		return instance.getBlockState(world, x, y, z);
	}

	public static int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		return instance.loadChunkAndGetBlockId(world, x, y, z);
	}

	public static boolean sendBlockChange(Player player, BlockCoords blockCoords) {
		return instance.sendBlockChange(player, blockCoords);
	}

	public static void close() {
		if (instance != null) {
			instance.close();
			instance = null;
		}
	}
}