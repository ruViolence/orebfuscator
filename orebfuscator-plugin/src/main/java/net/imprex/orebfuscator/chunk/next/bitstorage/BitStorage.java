package net.imprex.orebfuscator.chunk.next.bitstorage;

public interface BitStorage {

	int get(int index);

	int size();

	interface Writer {

		default void throwIfNotExhausted() {
			if (!isExhausted()) {
				throw new IllegalStateException("BitStorage.Writer is not exhausted but closed!");
			}
		}

		boolean isExhausted();

		void write(int value);

	}
}
