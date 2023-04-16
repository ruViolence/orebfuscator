package net.imprex.orebfuscator.chunk.next.palette;

import io.netty.buffer.ByteBuf;

public interface Palette {

	int bitsPerEntry();

	int valueFor(int index);

	int indexFor(int value);

	void write(ByteBuf buffer);

	int packetSize();

	public interface Builder {
		
		Builder add(int value);

		Palette build();
	}
}
