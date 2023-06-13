package net.imprex.orebfuscator.chunk.next.bitstorage.accessor;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.next.bitstorage.EmptyBitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.SimpleBitStorage;

public class BiomeBitStorage {

	public static void skip(ByteBuf buffer, int bitsPerEntry) {
		if (bitsPerEntry == 0) {
			EmptyBitStorage.skip(buffer);
		} else if (bitsPerEntry < 4) {
			SimpleBitStorage.skip(buffer);
		}
	}
}
