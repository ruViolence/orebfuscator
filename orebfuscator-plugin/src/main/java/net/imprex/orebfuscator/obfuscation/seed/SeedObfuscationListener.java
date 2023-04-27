package net.imprex.orebfuscator.obfuscation.seed;

import java.nio.ByteBuffer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class SeedObfuscationListener extends PacketAdapter {

	private final SeedObfuscationStrategy strategy = SeedObfuscationStrategy.createDynamically();
	private final long hash;

	public SeedObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.LOGIN);


		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(this);

		ByteBuffer buffer = ByteBuffer.wrap(orebfuscator.getOrebfuscatorConfig().systemHash());
		this.hash = buffer.getLong(0);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		event.setPacket(strategy.process(event.getPacket(), this.hash));
	}
}
