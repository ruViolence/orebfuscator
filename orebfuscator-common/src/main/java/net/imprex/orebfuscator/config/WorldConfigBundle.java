package net.imprex.orebfuscator.config;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

public interface WorldConfigBundle {

	BlockFlags blockFlags();

	Int2IntMap proximityReplaceMap();

	ObfuscationConfig obfuscation();

	ProximityConfig proximity();

	boolean needsObfuscation();

	boolean skipReadSectionIndex(int index);

	boolean skipProcessingSectionIndex(int index);

	int minSectionIndex();

	int maxSectionIndex();

	boolean shouldObfuscate(int y);

	int nextRandomObfuscationBlock(int y);

	int nextRandomProximityBlock(int y);
}
