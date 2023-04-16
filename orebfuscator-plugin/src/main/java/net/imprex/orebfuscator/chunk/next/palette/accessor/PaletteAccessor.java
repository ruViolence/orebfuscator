package net.imprex.orebfuscator.chunk.next.palette.accessor;

import java.util.function.IntFunction;

import net.imprex.orebfuscator.chunk.next.palette.Palette;

public interface PaletteAccessor {

	int getNumberOfUniqueValues();

	int getMaxBitsPerEntry();

	Palette createPalette(int size, IntFunction<int[]> valuesFactory);
}
