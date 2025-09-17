package net.minecraft.world;

/**
 * Extremely small subset of {@code World} required by the shadow world implementation.
 */
public abstract class World {
  public static final int NO_NEIGHBOR_UPDATES = 1 << 0;
  public static final int NO_LIGHTING = 1 << 1;
}
