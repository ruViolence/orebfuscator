package net.imprex.orebfuscator;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class OrebfuscatorPlayer {

	public static final ConcurrentMap<Player, OrebfuscatorPlayer> PLAYER_MAP = new MapMaker().weakKeys().makeMap();

	public static OrebfuscatorPlayer get(Player player) {
		OrebfuscatorPlayer playerData = PLAYER_MAP.computeIfAbsent(player, OrebfuscatorPlayer::new);
		playerData.updateWorld();
		return playerData;
	}

	private final WeakReference<Player> player;

	private final AtomicReference<World> world = new AtomicReference<>();
	private final Map<Long, Set<BlockPos>> chunks = new ConcurrentHashMap<>();

	private volatile Location location = new Location(null, 0, 0, 0);

	public OrebfuscatorPlayer(Player player) {
		this.player = new WeakReference<Player>(player);
	}

	public boolean hasLocationChanged(BiPredicate<Location, Location> equals) {
		if (this.player.refersTo(null)) {
			return false;
		}

		Location location = this.player.get().getLocation();
		if (equals.test(this.location, location)) {
			return false;
		}

		this.location = location;

		return true;
	}

	private void updateWorld() {
		if (this.player.refersTo(null)) {
			return;
		}
		
		World world = this.player.get().getWorld();
		if (!Objects.equal(this.world.getAndSet(world), world)) {
			this.chunks.clear();
		}
	}

	public void addChunk(int chunkX, int chunkZ, Set<BlockPos> blocks) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		System.out.println("add: " + chunkX + " " + chunkZ + " " + blocks.size());
		this.chunks.computeIfAbsent(key, k -> {
			return Collections.newSetFromMap(new ConcurrentHashMap<>());
		}).addAll(blocks);
	}

	public Set<BlockPos> getChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		return this.chunks.getOrDefault(key, Collections.emptySet());
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.remove(key);
	}
}
