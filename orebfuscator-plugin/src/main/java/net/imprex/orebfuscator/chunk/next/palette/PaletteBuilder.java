package net.imprex.orebfuscator.chunk.next.palette;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.imprex.orebfuscator.chunk.next.palette.accessor.PaletteAccessor;

public class PaletteBuilder implements Palette.Builder {

	private final PaletteAccessor paletteAccessor;

	private final BitSet byValue;
	private final List<Integer> byIndex;

	public PaletteBuilder(PaletteAccessor paletteAccessor) {
		this.paletteAccessor = paletteAccessor;

		this.byValue = new BitSet(paletteAccessor.getNumberOfUniqueValues());
		this.byIndex = new ArrayList<>();
	}

	@Override
	public PaletteBuilder add(int value) {
		if (!this.byValue.get(value)) {
			this.byValue.set(value);
			this.byIndex.add(value);
		}
		return this;
	}

	@Override
	public Palette build() {
		return this.paletteAccessor.createPalette(this.byIndex.size(), size -> {
			int[] byIndex = new int[size];
			for (int i = 0; i < size; i++) {
				byIndex[i] = this.byIndex.get(i);
			}
			return byIndex;
		});
	}
}
