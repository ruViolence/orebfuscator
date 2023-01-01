package net.imprex.orebfuscator.nms.v1_13_R1;

import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.server.v1_13_R1.Chunk;
import net.minecraft.server.v1_13_R1.HeightMap;

public class ReadOnlyChunkWrapper implements ReadOnlyChunk {

	private final Chunk chunk;

	ReadOnlyChunkWrapper(Chunk chunk) {
		this.chunk = chunk;
	}

	@Override
	public int getHeight(int x, int z) {
		return chunk.a(HeightMap.Type.WORLD_SURFACE, x, z);
	}

	@Override
	public int getBlockState(int x, int y, int z) {
		return NmsManager.getBlockId(chunk.getBlockData(x, y, z));
	}
}
