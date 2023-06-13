package net.imprex.orebfuscator.chunk.next.bitstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.ByteBufUtil;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public abstract class AbstractBitStorageTest {

	protected static final int STORAGE_SIZE = 65537; 

	protected static byte[] readIdentityBuffer(String path) {
		try {
			return Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected abstract int packetSize(int bitsPerEntry, int size);

	protected abstract BitStorage.Writer createWriter(ByteBuf buffer, int size, Palette palette);

	protected abstract BitStorage createReader(ByteBuf buffer, int size, Palette palette);

	protected abstract void skip(ByteBuf buffer);

	@Test
	public void testReadWriteSkip() {
		for (int bits = 1; bits < 32; bits++) {
			Palette palette = new IdentityPalette(bits);
			int maxValue = (1 << bits) - 1;

			int packetSize = packetSize(bits, STORAGE_SIZE);
			ByteBuf buffer = Unpooled.buffer(packetSize);

			BitStorage.Writer writer = createWriter(buffer, STORAGE_SIZE, palette);

			assertEquals(false, writer.isExhausted());
			assertThrows(IllegalStateException.class, () -> writer.throwIfNotExhausted());

			for (int i = 0; i < STORAGE_SIZE; i++) {
				assertEquals(false, writer.isExhausted());
				writer.write(i % maxValue);
			}

			assertEquals(true, writer.isExhausted());
			assertDoesNotThrow(() -> writer.throwIfNotExhausted());
			assertThrows(IllegalStateException.class, () -> writer.write(0));

			assertEquals(packetSize, buffer.readableBytes());

			BitStorage bitStorage = createReader(buffer, STORAGE_SIZE, palette);
			for (int i = 0; i < STORAGE_SIZE; i++) {
				assertEquals(i % maxValue, bitStorage.get(i));
			}

			assertEquals(0, buffer.readableBytes());
			assertEquals(STORAGE_SIZE, bitStorage.size());

			buffer.readerIndex(0);

			skip(buffer);

			assertEquals(0, buffer.readableBytes());
		}
	}

	@Test
	public void testFaweFix() {
		Palette palette = new IdentityPalette(16);

		int faweLength = (int) Math.ceil(STORAGE_SIZE / 64f);
		int fawePacketSize = faweLength * Long.BYTES;
		ByteBuf buffer = Unpooled.buffer(ByteBufUtil.getVarIntSize(faweLength) + fawePacketSize);

		ByteBufUtil.writeVarInt(buffer, faweLength);
		buffer.writerIndex(buffer.writerIndex() + fawePacketSize);

		BitStorage bitStorage = createReader(buffer, STORAGE_SIZE, palette);
		for (int i = 0; i < STORAGE_SIZE; i++) {
			assertEquals(0, bitStorage.get(i));
		}

		assertEquals(0, buffer.readableBytes());
	}

	protected static class IdentityPalette implements Palette {

		private final int bitsPerEntry;

		public IdentityPalette(int bitsPerEntry) {
			this.bitsPerEntry = bitsPerEntry;
		}

		@Override
		public int bitsPerEntry() {
			return this.bitsPerEntry;
		}

		@Override
		public int valueFor(int index) {
			return index;
		}

		@Override
		public int indexFor(int value) {
			return value;
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
