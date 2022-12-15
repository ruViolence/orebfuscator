package net.imprex.orebfuscator.chunk;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.World;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import net.imprex.orebfuscator.nms.v1_18_R1.WrappedClientboundLevelChunkPacketData;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ChunkStruct {

	public final World world;
	public final boolean isOverworld;

	public final int chunkX;
	public final int chunkZ;

	public final BitSet sectionMask;
	public final byte[] data;

	private final PacketContainer packet;
	private final WrappedClientboundLevelChunkPacketData packetData;

	public ChunkStruct(PacketContainer packet, World world) {
		this.packet = packet;
		StructureModifier<Integer> packetInteger = packet.getIntegers();

		this.world = world;
		this.isOverworld = world.getEnvironment() == World.Environment.NORMAL;

		this.chunkX = packetInteger.read(0);
		this.chunkZ = packetInteger.read(1);

		if (ChunkCapabilities.hasClientboundLevelChunkPacketData()) {
			this.packetData = new WrappedClientboundLevelChunkPacketData(packet);
			this.data = this.packetData.getBuffer();
		} else {
			this.packetData = null;
			this.data = packet.getByteArrays().read(0);
		}

		if (ChunkCapabilities.hasHeightBitMask()) {
			if (ChunkCapabilities.hasDynamicHeight()) {
				this.sectionMask = packet.getSpecificModifier(BitSet.class).read(0);
			} else {
				this.sectionMask = convertIntToBitSet(packetInteger.read(2));
			}
		} else {
			this.sectionMask = new BitSet();
			this.sectionMask.set(0, HeightAccessor.get(world).getSectionCount());
		}
	}

	public ChunkPosition getPosition() {
		return new ChunkPosition(this.world, this.chunkX, this.chunkZ);
	}

	public void updateFromResult(ObfuscationResult result) {
		this.setDataBuffer(result.getData());

		Set<BlockPos> blockEntities = result.getBlockEntities();
		if (!blockEntities.isEmpty()) {
			this.removeBlockEntityIf(blockEntities::contains);
		}
	}

	private void setDataBuffer(byte[] data) {
		if (this.packetData != null) {
			this.packetData.setBuffer(data);
		} else {
			this.packet.getByteArrays().write(0, data);
		}
	}

	private void removeBlockEntityIf(Predicate<BlockPos> predicate) {
		if (this.packetData != null) {
			this.packetData.removeBlockEntityIf(relativePostion -> 
			predicate.test(relativePostion.add(chunkX << 4, 0, chunkZ << 4)));
		} else {
			removeTileEntitiesFromPacket(this.packet, predicate);
		}
	}


	private void removeTileEntitiesFromPacket(PacketContainer packet, Predicate<BlockPos> predicate) {
		StructureModifier<List<NbtBase<?>>> packetNbtList = packet.getListNbtModifier();

		List<NbtBase<?>> tileEntities = packetNbtList.read(0);
		this.removeTileEntities(tileEntities, predicate);
		packetNbtList.write(0, tileEntities);
	}

	private void removeTileEntities(List<NbtBase<?>> tileEntities, Predicate<BlockPos> predicate) {
		for (Iterator<NbtBase<?>> iterator = tileEntities.iterator(); iterator.hasNext();) {
			NbtCompound tileEntity = (NbtCompound) iterator.next();

			int x = tileEntity.getInteger("x");
			int y = tileEntity.getInteger("y");
			int z = tileEntity.getInteger("z");

			BlockPos position = new BlockPos(x, y, z);
			if (predicate.test(position)) {
				iterator.remove();
			}
		}
	}

	public boolean isEmpty() {
		return this.sectionMask.isEmpty();
	}

	private BitSet convertIntToBitSet(int value) {
		BitSet bitSet = new BitSet();
		for (int index = 0; value != 0; index++) {
			if ((value & 1) == 1) {
				bitSet.set(index);
			}
			value >>>= 1;
		}
		return bitSet;
	}
}
