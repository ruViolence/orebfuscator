package net.imprex.orebfuscator.chunk.next.bitstorage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage.Writer;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class SimpleBitStorageTest extends AbstractBitStorageTest {

	private static final byte[] IDENTITY_BUFFER = readIdentityBuffer("src/test/resources/simple-bit-storage.bin");
	private static final int INDENTITY_BUFFER_ENTRIES = 65535;

	@Override
	protected int packetSize(int bitsPerEntry, int size) {
		return SimpleBitStorage.packetSize(bitsPerEntry, size);
	}

	@Override
	protected Writer createWriter(ByteBuf buffer, int size, Palette palette) {
		return new SimpleBitStorage.Writer(buffer, size, palette);
	}

	@Override
	protected BitStorage createReader(ByteBuf buffer, int size, Palette palette) {
		return SimpleBitStorage.read(buffer, size, palette);
	}

	@Override
	protected void skip(ByteBuf buffer) {
		SimpleBitStorage.skip(buffer);
	}

	@Test
	public void testReadWriteIdentity() {
		Palette palette = new IdentityPalette(16);

		ByteBuf buffer = Unpooled.wrappedBuffer(IDENTITY_BUFFER).asReadOnly();
		BitStorage bitStorage = SimpleBitStorage.read(buffer, INDENTITY_BUFFER_ENTRIES, palette);
		for (int i = 0; i < INDENTITY_BUFFER_ENTRIES; i++) {
			assertEquals(i, bitStorage.get(i));
		}

		byte[] array = new byte[IDENTITY_BUFFER.length];
		ByteBuf writeBuffer = Unpooled.wrappedBuffer(array);
		writeBuffer.writerIndex(0);

		BitStorage.Writer writer = new SimpleBitStorage.Writer(writeBuffer, INDENTITY_BUFFER_ENTRIES, palette);
		for (int i = 0; i < INDENTITY_BUFFER_ENTRIES; i++) {
			writer.write(i);
		}

		assertArrayEquals(IDENTITY_BUFFER, array);
	}
}
