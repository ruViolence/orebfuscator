package net.imprex.orebfuscator.util;

public enum ChunkDirection {

	NORTH(0, -1, 0, 15),
	EAST(1, 0, 0, 0),
	SOUTH(0, 1, 15, 0),
	WEST(-1, 0, 15, 15);

	private final int offsetX;
	private final int offsetZ;

	private final int hashStartX;
	private final int hashStartZ;

	private ChunkDirection(int offsetX, int offsetZ, int hashStartX, int hashStartZ) {
		this.offsetX = offsetX;
		this.offsetZ = offsetZ;
		this.hashStartX = hashStartX;
		this.hashStartZ = hashStartZ;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public int getOffsetZ() {
		return offsetZ;
	}

	public int getHashStartX() {
		return hashStartX;
	}

	public int getHashStartZ() {
		return hashStartZ;
	}

	public ChunkDirection rotate() {
		switch (this) {
			case NORTH:
				return EAST;
			case EAST:
				return SOUTH;
			case SOUTH:
				return WEST;
			case WEST:
				return NORTH;
			default:
				throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
		}
	}

	public static ChunkDirection fromPosition(ChunkPosition position, int targetX, int targetZ) {
		int offsetX = (targetX >> 4) - position.getX();
		int offsetZ = (targetZ >> 4) - position.getZ();

		if (offsetX == 0 && offsetZ == -1) {
			return NORTH;
		} else if (offsetX == 1 && offsetZ == 0) {
			return EAST;
		} else if (offsetX == 0 && offsetZ == 1) {
			return SOUTH;
		} else if (offsetX == -1 && offsetZ == 0) {
			return WEST;
		}

		throw new IllegalArgumentException(String.format("invalid offset (origin: %s, x: %d, z: %d)", position, targetX, targetZ));
	}
}
