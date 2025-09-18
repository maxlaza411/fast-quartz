package net.minecraft.world.ticks;

/** Minimal subset of Minecraft's {@code TickPriority}. */
public enum TickPriority {
  EXTREMELY_LOW(6),
  VERY_LOW(5),
  LOW(4),
  NORMAL(3),
  HIGH(2),
  VERY_HIGH(1),
  EXTREMELY_HIGH(0);

  private final int value;

  TickPriority(int value) {
    this.value = value;
  }

  /** Lower values run earlier within the same tick. */
  public int value() {
    return value;
  }
}
