package net.imprex.orebfuscator.chunk.next.bitstorage;

public interface BitStorage {

	int get(int index);

	int size();

	interface Writer {
		
		void write(int value);

	}
}
