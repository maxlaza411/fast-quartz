package dev.fastquartz.fastquartz.engine;

import java.util.Objects;

/** Immutable record describing a power change observed at a block position. */
public record StateChange(long tick, BlockPos position, int previousPower, int newPower) {
  public StateChange {
    Objects.requireNonNull(position, "position");
    previousPower = RedstoneMath.requirePowerRange(previousPower, "previous");
    newPower = RedstoneMath.requirePowerRange(newPower, "new");
  }

  /** Returns {@code true} if the power increased. */
  public boolean isIncrease() {
    return newPower > previousPower;
  }

  /** Returns {@code true} if the power decreased. */
  public boolean isDecrease() {
    return newPower < previousPower;
  }
}
