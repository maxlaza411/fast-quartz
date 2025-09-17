package net.minecraft.util.math;

/** Minimal subset of Minecraft's {@code Direction} enum. */
public enum Direction {
  DOWN(0, -1, 0),
  UP(0, 1, 0),
  NORTH(0, 0, -1),
  SOUTH(0, 0, 1),
  WEST(-1, 0, 0),
  EAST(1, 0, 0);

  private final int offsetX;
  private final int offsetY;
  private final int offsetZ;

  Direction(int offsetX, int offsetY, int offsetZ) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
  }

  public int getOffsetX() {
    return this.offsetX;
  }

  public int getOffsetY() {
    return this.offsetY;
  }

  public int getOffsetZ() {
    return this.offsetZ;
  }

  /** Returns the opposite direction. */
  public Direction getOpposite() {
    return switch (this) {
      case DOWN -> UP;
      case UP -> DOWN;
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case WEST -> EAST;
      case EAST -> WEST;
    };
  }
}
