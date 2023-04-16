package net.imprex.orebfuscator.chunk.next.palette.accessor;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.next.palette.IndirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.SingleValuedPalette;

public class BiomePaletteAccessor {

	public static void skip(ByteBuf buffer, int bitsPerEntry) {
		if (bitsPerEntry == 0) {
			SingleValuedPalette.skip(buffer);
		} else if (bitsPerEntry < 4) {
			IndirectPalette.skip(buffer);
		}
	}
}
