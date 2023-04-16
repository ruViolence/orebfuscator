package net.imprex.orebfuscator.chunk.next.palette;

import net.imprex.orebfuscator.chunk.next.palette.Palette.Builder;

public class EmptyPaletteBuilder implements Palette.Builder {

	private final Palette palette;

	public EmptyPaletteBuilder(Palette palette) {
		this.palette = palette;
	}

	@Override
	public Builder add(int value) {
		return this;
	}

	@Override
	public Palette build() {
		return this.palette;
	}
}
