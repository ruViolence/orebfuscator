package net.imprex.orebfuscator.obfuscation;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketPostAdapter;
import com.comphenix.protocol.reflect.StructureModifier;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.player.PlayerDataStorage;
import net.imprex.orebfuscator.proximityhider.ProximityHider;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.PermissionUtil;

public abstract class ObfuscationListener extends PacketAdapter {

	private final OrebfuscatorConfig config;
	private final PlayerDataStorage playerDataStorage;
	private final ObfuscationSystem obfuscationSystem;
	private final ProximityHider proximityHider;

	protected final ProtocolManager protocolManager;

	public ObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK, PacketType.Play.Server.UNLOAD_CHUNK);

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.playerDataStorage = orebfuscator.getPlayerDataStorage();
		this.obfuscationSystem = orebfuscator.getObfuscationSystem();
		this.proximityHider = orebfuscator.getProximityHider();

		this.protocolManager = ProtocolLibrary.getProtocolManager();
	}

	protected abstract void preChunkProcessing(PacketEvent event);

	protected abstract void postChunkProcessing(PacketEvent event);

	protected abstract void discardChunkPacket(PacketEvent event);

	public abstract void unregister();

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (PermissionUtil.canDeobfuscate(player) || !config.needsObfuscation(player.getWorld())) {
			return;
		}

		if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
			this.onChunkUnload(player, event);
		} else if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {
			this.onChunkLoad(player, event);
		}
	}

	private void onChunkUnload(Player player, PacketEvent event) {
		StructureModifier<Integer> integers = event.getPacket().getIntegers();
		int chunkX = integers.read(0);
		int chunkZ = integers.read(1);

		final ChunkPosition position = new ChunkPosition(player.getWorld(), chunkX, chunkZ);
		this.playerDataStorage.unloadChunk(player, position);
	}

	private void onChunkLoad(Player player, PacketEvent event) {
		ChunkStruct struct = new ChunkStruct(event.getPacket(), player.getWorld());
		if (struct.isEmpty()) {
			return;
		}

		final ChunkPosition position = struct.getPosition();
		if (this.playerDataStorage.processChunk(player, position)) {
			this.discardChunkPacket(event);
			return;
		}

		this.preChunkProcessing(event);

		this.obfuscationSystem.obfuscate(struct).whenComplete((chunk, throwable) -> {
			if (throwable != null) {
				OFCLogger.error(String.format("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]",
						struct.world.getName(), struct.chunkX, struct.chunkZ), throwable);
			} else if (chunk != null) {
				if (this.playerDataStorage.preSendChunk(player, position)) {
					this.discardChunkPacket(event);
					return;
				}

				struct.updateFromResult(chunk);

				this.createPostListener(event, () -> {
					playerDataStorage.postSendChunk(player, position, chunk.getProximityBlocks());
					this.proximityHider.queuePlayerUpdate(player);
				});
			} else {
				OFCLogger.warn(
						String.format("skipping chunk[world=%s, x=%d, z=%d] because obfuscation result is missing",
								struct.world.getName(), struct.chunkX, struct.chunkZ));
			}

			this.postChunkProcessing(event);
		});

	}

	private void createPostListener(PacketEvent event, Runnable listener) {
		event.getNetworkMarker().addPostListener(new PacketPostAdapter(this.plugin) {

			@Override
			public void onPostEvent(PacketEvent event) {
				listener.run();
			}
		});
	}
}
