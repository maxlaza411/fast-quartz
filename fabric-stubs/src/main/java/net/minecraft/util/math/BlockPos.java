package net.minecraft.util.math;

import java.util.Objects;

/** Minimal immutable block position used by tests and engine stubs. */
public final class BlockPos {
  private final int x;
  private final int y;
  private final int z;

  public BlockPos(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getZ() {
    return this.z;
  }

  /** Returns a new position offset by the given direction. */
  public BlockPos offset(Direction direction) {
    return new BlockPos(
        this.x + direction.getOffsetX(),
        this.y + direction.getOffsetY(),
        this.z + direction.getOffsetZ());
  }

  /** Returns a new position translated by the specified delta on each axis. */
  public BlockPos add(int dx, int dy, int dz) {
    if (dx == 0 && dy == 0 && dz == 0) {
      return this;
    }
    return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockPos other)) {
      return false;
    }
    return this.x == other.x && this.y == other.y && this.z == other.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.x, this.y, this.z);
  }

  @Override
  public String toString() {
    return "BlockPos{" + "x=" + this.x + ", y=" + this.y + ", z=" + this.z + '}';
  }
}
