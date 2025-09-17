package dev.fastquartz.faststone.engine;

/** Immutable 3D block position with deterministic ordering semantics (Y, then Z, then X). */
public record BlockPos(int x, int y, int z) implements Comparable<BlockPos> {
  @Override
  public int compareTo(BlockPos other) {
    int yCompare = Integer.compare(this.y, other.y);
    if (yCompare != 0) {
      return yCompare;
    }
    int zCompare = Integer.compare(this.z, other.z);
    if (zCompare != 0) {
      return zCompare;
    }
    return Integer.compare(this.x, other.x);
  }

  /** Returns a new position offset by the given delta in each axis. */
  public BlockPos offset(int dx, int dy, int dz) {
    if (dx == 0 && dy == 0 && dz == 0) {
      return this;
    }
    return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
  }
}
