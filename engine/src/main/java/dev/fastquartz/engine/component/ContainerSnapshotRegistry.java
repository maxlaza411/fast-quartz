package dev.fastquartz.engine.component;

import dev.fastquartz.engine.world.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Caches comparator/container signals for the duration of a micro-phase.
 *
 * <p>The registry exposes deterministic snapshots: the first time a given position is observed the
 * supplied reader is invoked and the value is memoised. Subsequent reads return the same cached
 * value even if the underlying container changes. The caller is responsible for resetting or
 * discarding the registry between ticks.
 */
public final class ContainerSnapshotRegistry {
  private final Map<BlockPos, Integer> snapshots = new HashMap<>();

  /** Retrieves the cached signal for {@code pos}, loading it via {@code reader} if absent. */
  public int snapshot(BlockPos pos, IntSupplier reader) {
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(reader, "reader");
    Integer cached = snapshots.get(pos);
    if (cached != null) {
      return cached;
    }
    int value = reader.getAsInt();
    snapshots.put(pos, value);
    return value;
  }

  /** Clears all cached entries. */
  public void clear() {
    snapshots.clear();
  }
}
