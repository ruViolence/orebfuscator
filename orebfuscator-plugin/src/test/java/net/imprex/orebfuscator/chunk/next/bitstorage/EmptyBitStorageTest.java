package net.imprex.orebfuscator.chunk.next.bitstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class EmptyBitStorageTest {

	private static final int STORAGE_SIZE = 4095;
	private static final int VALUE = 7;

	@Test
	public void testReadWriteSkip() {
		Palette validPalette = new EmptyTestPalette(0);
		Palette invalidPalette = new EmptyTestPalette(1);

		int packetSize = EmptyBitStorage.packetSize();
		ByteBuf buffer = Unpooled.buffer(packetSize);

		BitStorage.Writer writer = new EmptyBitStorage.Writer(buffer);

		assertEquals(true, writer.isExhausted());
		assertDoesNotThrow(() -> writer.throwIfNotExhausted());

		assertThrows(UnsupportedOperationException.class, () -> writer.write(0));
		assertEquals(packetSize, buffer.readableBytes());

		assertThrows(IllegalArgumentException.class, () -> EmptyBitStorage.read(buffer, STORAGE_SIZE, invalidPalette));
		BitStorage bitStorage = EmptyBitStorage.read(buffer, STORAGE_SIZE, validPalette);

		for (int i = 0; i < STORAGE_SIZE; i++) {
			assertEquals(VALUE, bitStorage.get(i));
		}

		assertEquals(0, buffer.readableBytes());
		assertEquals(STORAGE_SIZE, bitStorage.size());

		buffer.readerIndex(0);

		EmptyBitStorage.skip(buffer);

		assertEquals(0, buffer.readableBytes());
	}

	private class EmptyTestPalette implements Palette {

		private final int bitsPerEntry;

		public EmptyTestPalette(int bitsPerEntry) {
			this.bitsPerEntry = bitsPerEntry;
		}

		@Override
		public int bitsPerEntry() {
			return this.bitsPerEntry;
		}

		@Override
		public int valueFor(int index) {
			return VALUE;
		}

		@Override
		public int indexFor(int value) {
			return 0;
		}

		@Override
		public void write(ByteBuf buffer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int packetSize() {
			throw new UnsupportedOperationException();
		}
	}
}
