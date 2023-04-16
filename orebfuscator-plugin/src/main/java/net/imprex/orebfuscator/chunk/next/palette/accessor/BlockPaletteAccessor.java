package net.imprex.orebfuscator.chunk.next.palette.accessor;

import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.palette.DirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.EmptyPaletteBuilder;
import net.imprex.orebfuscator.chunk.next.palette.IndirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.palette.PaletteBuilder;
import net.imprex.orebfuscator.chunk.next.palette.SingleValuedPalette;

public class BlockPaletteAccessor implements PaletteAccessor {

	public static final BlockPaletteAccessor INSTANCE = new BlockPaletteAccessor();

	public static void skip(ByteBuf buffer, int bitsPerEntry) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			SingleValuedPalette.skip(buffer);
		} else if (bitsPerEntry < 9) {
			IndirectPalette.skip(buffer);
		} else {
			DirectPalette.skip(buffer);
		}
	}

	public static Palette read(ByteBuf buffer, int bitsPerEntry) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			return SingleValuedPalette.read(buffer);
		} else if (bitsPerEntry < 9) {
			return IndirectPalette.read(buffer, Math.max(4, bitsPerEntry), INSTANCE);
		} else {
			DirectPalette.skip(buffer);
			return INSTANCE.directPalette;
		}
	}

	public static Palette.Builder builder(Palette palette) {
		if (palette instanceof DirectPalette) {
			return INSTANCE.emptyBuilder;
		} else {
			return new PaletteBuilder(INSTANCE);
		}
	}

	private final DirectPalette directPalette = new DirectPalette(this);
	private final EmptyPaletteBuilder emptyBuilder = new EmptyPaletteBuilder(directPalette);

	@Override
	public int getNumberOfUniqueValues() {
		return NmsInstance.getTotalBlockCount();
	}

	@Override
	public int getMaxBitsPerEntry() {
		return NmsInstance.getMaxBitsPerBlock();
	}

	@Override
	public Palette createPalette(int size, IntFunction<int[]> valuesFactory) {
		if (size < 256) {
			if (ChunkCapabilities.hasSingleValuePalette() && size == 1) {
				int[] values = valuesFactory.apply(size);
				return new SingleValuedPalette(values[0]);
			} else {
				int[] values = valuesFactory.apply(size);
				int bitsPerEntry = 32 - Integer.numberOfLeadingZeros(values.length);
				return new IndirectPalette(values, Math.max(4, bitsPerEntry), this);
			}
		}
		return directPalette;
	}
}
