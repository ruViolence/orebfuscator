package net.imprex.orebfuscator.obfuscation;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;

import com.comphenix.protocol.events.NetworkMarker;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationListenerSync extends ObfuscationListener {

	public ObfuscationListenerSync(Orebfuscator orebfuscator) {
		super(orebfuscator);
		this.protocolManager.addPacketListener(this);
	}

	@Override
	protected void preChunkProcessing(PacketEvent event) {
		event.setCancelled(true);
	}

	@Override
	protected void postChunkProcessing(PacketEvent event) {
		final Player player = event.getPlayer();
		final PacketContainer packet = event.getPacket();
		final NetworkMarker networkMarker = event.getNetworkMarker();

		try {
			this.protocolManager.sendServerPacket(player, packet, networkMarker, false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void discardChunkPacket(PacketEvent event) {
		event.setCancelled(true);
	}

	@Override
	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}
}
