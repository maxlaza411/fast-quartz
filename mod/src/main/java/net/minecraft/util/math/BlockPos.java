package net.minecraft.util.math;

/** Minimal stub of Minecraft's {@code BlockPos}. */
public record BlockPos(int x, int y, int z) {
  public BlockPos {
    // Vanilla worlds clamp Y to a finite range; the stub simply records the coordinates.
  }

  public static BlockPos of(int x, int y, int z) {
    return new BlockPos(x, y, z);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getZ() {
    return z;
  }
}
