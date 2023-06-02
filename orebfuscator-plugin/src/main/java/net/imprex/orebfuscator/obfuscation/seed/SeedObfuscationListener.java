package net.imprex.orebfuscator.obfuscation.seed;

import java.nio.ByteBuffer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class SeedObfuscationListener extends PacketAdapter {

	private final long hash;

	public SeedObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.LOGIN, PacketType.Play.Server.RESPAWN);


		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(this);

		ByteBuffer buffer = ByteBuffer.wrap(orebfuscator.getOrebfuscatorConfig().systemHash());
		this.hash = buffer.getLong(0);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPacketType() == PacketType.Play.Server.LOGIN) {
			event.setPacket(SeedObfuscationStrategy.DYNAMIC_LOGIN.process(event.getPacket(), this.hash));
		} else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
			event.setPacket(SeedObfuscationStrategy.DYNAMIC_RESPAWN.process(event.getPacket(), this.hash));
		}
	}
}
