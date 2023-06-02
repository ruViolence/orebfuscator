package net.imprex.orebfuscator.obfuscation.seed;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;

@FunctionalInterface
public interface SeedObfuscationStrategy {

	static final SeedObfuscationStrategy NOOP = (packet, hash) -> packet;
	static final SeedObfuscationStrategy DYNAMIC_LOGIN = createDynamicFor(PacketType.Play.Server.LOGIN);
	static final SeedObfuscationStrategy DYNAMIC_RESPAWN = createDynamicFor(PacketType.Play.Server.RESPAWN);

	static SeedObfuscationStrategy createDynamicFor(PacketType packetType) {
		if (packetType != PacketType.Play.Server.LOGIN || packetType != PacketType.Play.Server.RESPAWN) {
			throw new IllegalArgumentException("unsupported packet type: " + packetType);
		}

		final Class<?> packetClass = packetType.getPacketClass();

		if (packetClass.isRecord()) {
			final RecordComponent[] components = packetClass.getRecordComponents();
			final Class<?>[] componentClasses = new Class<?>[components.length];

			int longIndex = -1;
			for (int i = 0; i < components.length; i++) {
				Class<?> componentClass = components[i].getType();

				if (longIndex == -1 && componentClass == long.class) {
					longIndex = i;
				}

				componentClasses[i] = componentClass;
			}

			final int hashIndex = longIndex;
			if (hashIndex == -1) {
				return NOOP;
			}

			final ConstructorAccessor constructorAccessor = Accessors.getConstructorAccessor(
					packetClass, componentClasses);

			final FieldAccessor[] fieldAccessors = Arrays.stream(packetClass.getDeclaredFields())
					.map(Accessors::getFieldAccessor)
					.toArray(FieldAccessor[]::new);

			return (packet, hash) -> {
				Object handle = packet.getHandle();
				Object[] arguments = new Object[components.length];

				for (int i = 0; i < components.length; i++) {
					if (hashIndex == i) {
						arguments[i] = hash;
					} else {
						arguments[i] = fieldAccessors[i].get(handle);
					}
				}

				handle = constructorAccessor.invoke(arguments);
				return new PacketContainer(packetType, handle);
			};
		} else {
			try {
				Accessors.getFieldAccessor(packetClass, long.class, true);
			} catch (Exception e) {
				return NOOP;
			}

			return (packet, hash) -> {
				packet.getLongs().write(0, hash);
				return packet;
			};
		}
	}

	PacketContainer process(PacketContainer packet, long hash);
}
