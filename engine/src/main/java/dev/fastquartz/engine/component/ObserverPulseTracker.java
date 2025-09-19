package dev.fastquartz.engine.component;

import dev.fastquartz.engine.world.BlockPos;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Tracks observer emissions to ensure Stable-logic coalescing behaviour. */
public final class ObserverPulseTracker {
  private long tick = Long.MIN_VALUE;
  private final Set<ObserverKey> emitted = new HashSet<>();

  /**
   * Returns {@code true} if an observer at {@code observerPos} may emit for {@code sourcePos}
   * during {@code currentTick}. Only the first call per tick for a given observer/source pair will
   * succeed.
   */
  public boolean shouldEmit(long currentTick, BlockPos observerPos, BlockPos sourcePos) {
    Objects.requireNonNull(observerPos, "observerPos");
    Objects.requireNonNull(sourcePos, "sourcePos");
    if (currentTick != tick) {
      tick = currentTick;
      emitted.clear();
    }
    return emitted.add(new ObserverKey(observerPos, sourcePos));
  }

  private record ObserverKey(BlockPos observer, BlockPos source) {
    // value object for coalescing keys
  }
}
