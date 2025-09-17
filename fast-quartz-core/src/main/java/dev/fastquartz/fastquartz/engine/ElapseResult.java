package dev.fastquartz.fastquartz.engine;

import java.util.List;

/** Result of advancing the simulation by a number of ticks. */
public record ElapseResult(long startTick, long endTick, List<StateChange> changes) {
  public ElapseResult {
    if (endTick < startTick) {
      throw new IllegalArgumentException("endTick must be >= startTick");
    }
    changes = List.copyOf(changes);
  }

  /** Returns how many ticks elapsed during the solve. */
  public long ticksAdvanced() {
    return endTick - startTick;
  }
}
