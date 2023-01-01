package net.imprex.orebfuscator.nms;

public interface ReadOnlyChunk {

	int getHeight(int x, int z);

	int getBlockState(int x, int y, int z);
}
