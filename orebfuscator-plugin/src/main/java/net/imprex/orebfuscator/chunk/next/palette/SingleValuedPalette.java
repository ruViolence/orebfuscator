package net.imprex.orebfuscator.chunk.next.palette;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ByteBufUtil;

public class SingleValuedPalette implements Palette {

	public static void skip(ByteBuf buffer) {
		ByteBufUtil.skipVarInt(buffer);
	}

	public static Palette read(ByteBuf buffer) {
		int value = ByteBufUtil.readVarInt(buffer);
		return new SingleValuedPalette(value);
	}

	private final int value;

	public SingleValuedPalette(int value) {
		this.value = value;
	}

	@Override
	public int bitsPerEntry() {
		return 0;
	}

	@Override
	public int valueFor(int index) {
		if (index != 0) {
			throw new IllegalArgumentException("Invalid index, single valued palette only has one entry!");
		}
		return this.value;
	}

	@Override
	public int indexFor(int value) {
		if (value != this.value) {
			throw new IllegalArgumentException("Invalid value, single valued palette only has one entry!");
		}
		return 0;
	}

	@Override
	public void write(ByteBuf buffer) {
		ByteBufUtil.writeVarInt(buffer, this.value);
	}

	@Override
	public int packetSize() {
		return ByteBufUtil.getVarIntSize(this.value);
	}
}
