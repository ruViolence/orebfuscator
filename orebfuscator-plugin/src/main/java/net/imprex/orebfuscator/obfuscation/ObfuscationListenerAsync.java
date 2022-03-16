package net.imprex.orebfuscator.obfuscation;

import java.lang.reflect.InvocationTargetException;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationListenerAsync extends ObfuscationListener {

	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;

	public ObfuscationListenerAsync(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.asynchronousManager = this.protocolManager.getAsynchronousManager();
		this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);
		this.asyncListenerHandler.start(orebfuscator.getOrebfuscatorConfig().advanced().protocolLibThreads());
	}

	@Override
	protected void preChunkProcessing(PacketEvent event) {
		event.getAsyncMarker().incrementProcessingDelay();
	}

	@Override
	protected void postChunkProcessing(PacketEvent event) {
		this.asynchronousManager.signalPacketTransmission(event);
	}

	@Override
	protected void discardChunkPacket(PacketEvent event) {
		event.setCancelled(true);
		event.getAsyncMarker().setAsyncCancelled(true);

		this.asynchronousManager.signalPacketTransmission(event);
	}

	@Override
	protected Runnable deferUnloadPacket(PacketEvent event) {
		PacketContainer unloadPacket = event.getPacket().deepClone();

		event.setCancelled(true);
		event.getAsyncMarker().setAsyncCancelled(true);
		this.asynchronousManager.signalPacketTransmission(event);

		return () -> {
			try {
				this.protocolManager.sendServerPacket(event.getPlayer(), unloadPacket, event.getNetworkMarker(), false);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		};
	}

	@Override
	public void unregister() {
		this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
	}
}
