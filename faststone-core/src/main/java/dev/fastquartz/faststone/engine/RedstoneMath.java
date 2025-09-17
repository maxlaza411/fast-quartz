package dev.fastquartz.faststone.engine;

/** Common helper utilities for redstone signal processing. */
public final class RedstoneMath {
  public static final int MIN_POWER = 0;
  public static final int MAX_POWER = 15;

  private RedstoneMath() {}

  /** Clamps the provided value to the valid redstone power range (0-15 inclusive). */
  public static int clampPower(int value) {
    if (value < MIN_POWER) {
      return MIN_POWER;
    }
    if (value > MAX_POWER) {
      return MAX_POWER;
    }
    return value;
  }

  /** Ensures the provided value lies within the valid redstone power range. */
  public static int requirePowerRange(int value, String name) {
    if (value < MIN_POWER || value > MAX_POWER) {
      throw new IllegalArgumentException(
          String.format(
              "%s power must be between %d and %d (was %d)", name, MIN_POWER, MAX_POWER, value));
    }
    return value;
  }
}
