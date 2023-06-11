package net.imprex.orebfuscator.player;

import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.collect.MapMaker;

import net.imprex.orebfuscator.Orebfuscator;

public class OrebfuscatorPlayerMap implements Listener {

	private final Orebfuscator orebfuscator;

	private final ConcurrentMap<Player, OrebfuscatorPlayer> internalMap = new MapMaker().weakKeys().makeMap();

	public OrebfuscatorPlayerMap(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		if (orebfuscator.getOrebfuscatorConfig().proximityEnabled()) {
			Bukkit.getPluginManager().registerEvents(this, orebfuscator);
		}
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		this.internalMap.put(player, new OrebfuscatorPlayer(orebfuscator, player));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.internalMap.remove(event.getPlayer());
	}

	public OrebfuscatorPlayer get(Player player) {
		OrebfuscatorPlayer orebfuscatorPlayer = this.internalMap.get(player);
		if (orebfuscatorPlayer != null) {
			orebfuscatorPlayer.updateWorld();
		}
		return orebfuscatorPlayer;
	}
}
