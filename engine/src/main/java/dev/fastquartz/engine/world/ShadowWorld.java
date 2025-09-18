package dev.fastquartz.engine.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Shadow overlay for block mutations performed during a simulation tick.
 *
 * <p>The overlay buffers writes on a per chunk-section basis (16×16×16 blocks). Reads first consult
 * the buffered changes; if a position has not been touched, data is sourced from the backing
 * delegate. When {@link #commit()} is invoked all pending block state changes, scheduled ticks and
 * neighbour notifications are applied to the delegate in a deterministic order.
 */
public final class ShadowWorld {
  private static final int SECTION_SIZE = 16;
  private static final int LOCAL_MASK = SECTION_SIZE - 1;

  private static final Comparator<SectionPos> SECTION_ORDER =
      Comparator.comparingInt(SectionPos::y)
          .thenComparingInt(SectionPos::z)
          .thenComparingInt(SectionPos::x);

  private final Delegate delegate;
  private final NavigableMap<SectionPos, SectionChanges> sectionChanges =
      new TreeMap<>(SECTION_ORDER);
  private final List<ScheduledTick> scheduledTicks = new ArrayList<>();
  private final List<NeighborNotification> neighborNotifications = new ArrayList<>();

  public ShadowWorld(Delegate delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  /** Returns the block state bits at the supplied position. */
  public int getBlockStateBits(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    SectionPos section = toSectionPos(pos);
    SectionChanges changes = sectionChanges.get(section);
    if (changes != null) {
      int localIndex = toLocalIndex(pos);
      int overlayIndex = changes.indexOf(localIndex);
      if (overlayIndex >= 0) {
        return changes.stateBitsAt(overlayIndex);
      }
    }
    return delegate.getBlockStateBits(pos);
  }

  /** Buffers a block state update for the supplied position. */
  public void setBlockStateBits(BlockPos pos, int stateBits) {
    Objects.requireNonNull(pos, "pos");
    SectionPos section = toSectionPos(pos);
    int localIndex = toLocalIndex(pos);
    SectionChanges changes = sectionChanges.get(section);
    int overlayIndex = changes != null ? changes.indexOf(localIndex) : -1;
    if (overlayIndex >= 0 && changes.stateBitsAt(overlayIndex) == stateBits) {
      return; // already buffered with identical value
    }

    int baseState = delegate.getBlockStateBits(pos);
    if (stateBits == baseState) {
      if (changes != null && overlayIndex >= 0) {
        changes.removeAt(overlayIndex);
        if (changes.isEmpty()) {
          sectionChanges.remove(section);
        }
      }
      return;
    }

    if (changes == null) {
      changes = new SectionChanges();
      sectionChanges.put(section, changes);
    }
    changes.put(localIndex, stateBits);
  }

  /** Records a neighbour notification to be delivered at commit time. */
  public void markNeighborChanged(BlockPos pos, BlockPos source) {
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(source, "source");
    neighborNotifications.add(new NeighborNotification(pos, source));
  }

  /** Records a scheduled tick to be emitted during commit. */
  public void scheduleTick(BlockPos pos, int delayTicks, int priority) {
    Objects.requireNonNull(pos, "pos");
    scheduledTicks.add(new ScheduledTick(pos, delayTicks, priority));
  }

  /** Applies all buffered mutations to the delegate in deterministic order. */
  public void commit() {
    for (Map.Entry<SectionPos, SectionChanges> entry : sectionChanges.entrySet()) {
      SectionPos section = entry.getKey();
      SectionChanges changes = entry.getValue();
      for (int i = 0; i < changes.size(); i++) {
        int localIndex = changes.localIndexAt(i);
        int stateBits = changes.stateBitsAt(i);
        BlockPos absolutePos = toBlockPos(section, localIndex);
        delegate.setBlockStateBits(absolutePos, stateBits);
      }
    }
    sectionChanges.clear();

    if (!scheduledTicks.isEmpty()) {
      for (ScheduledTick tick : scheduledTicks) {
        delegate.scheduleTick(tick.pos(), tick.delayTicks(), tick.priority());
      }
      scheduledTicks.clear();
    }

    if (!neighborNotifications.isEmpty()) {
      for (NeighborNotification notification : neighborNotifications) {
        delegate.markNeighborChanged(notification.pos(), notification.source());
      }
      neighborNotifications.clear();
    }
  }

  private static SectionPos toSectionPos(BlockPos pos) {
    int sectionX = Math.floorDiv(pos.x(), SECTION_SIZE);
    int sectionY = Math.floorDiv(pos.y(), SECTION_SIZE);
    int sectionZ = Math.floorDiv(pos.z(), SECTION_SIZE);
    return new SectionPos(sectionX, sectionY, sectionZ);
  }

  private static int toLocalIndex(BlockPos pos) {
    int localX = Math.floorMod(pos.x(), SECTION_SIZE);
    int localY = Math.floorMod(pos.y(), SECTION_SIZE);
    int localZ = Math.floorMod(pos.z(), SECTION_SIZE);
    return (localY << 8) | (localZ << 4) | localX;
  }

  private static BlockPos toBlockPos(SectionPos section, int localIndex) {
    int localX = localIndex & LOCAL_MASK;
    int localZ = (localIndex >> 4) & LOCAL_MASK;
    int localY = (localIndex >> 8) & LOCAL_MASK;
    int worldX = (section.x() << 4) + localX;
    int worldY = (section.y() << 4) + localY;
    int worldZ = (section.z() << 4) + localZ;
    return new BlockPos(worldX, worldY, worldZ);
  }

  private record SectionPos(int x, int y, int z) {}

  private static final class SectionChanges {
    private int[] localIndices = new int[4];
    private int[] stateBits = new int[4];
    private int size;

    int size() {
      return size;
    }

    int localIndexAt(int index) {
      return localIndices[index];
    }

    int stateBitsAt(int index) {
      return stateBits[index];
    }

    int indexOf(int localIndex) {
      return java.util.Arrays.binarySearch(localIndices, 0, size, localIndex);
    }

    void put(int localIndex, int value) {
      int idx = indexOf(localIndex);
      if (idx >= 0) {
        stateBits[idx] = value;
        return;
      }
      ensureCapacity(size + 1);
      int insertIdx = -(idx + 1);
      System.arraycopy(localIndices, insertIdx, localIndices, insertIdx + 1, size - insertIdx);
      System.arraycopy(stateBits, insertIdx, stateBits, insertIdx + 1, size - insertIdx);
      localIndices[insertIdx] = localIndex;
      stateBits[insertIdx] = value;
      size++;
    }

    boolean removeAt(int index) {
      if (index < 0 || index >= size) {
        return false;
      }
      int elementsToMove = size - index - 1;
      if (elementsToMove > 0) {
        System.arraycopy(localIndices, index + 1, localIndices, index, elementsToMove);
        System.arraycopy(stateBits, index + 1, stateBits, index, elementsToMove);
      }
      size--;
      return true;
    }

    boolean isEmpty() {
      return size == 0;
    }

    private void ensureCapacity(int desiredCapacity) {
      if (desiredCapacity <= localIndices.length) {
        return;
      }
      int newCapacity = Math.max(desiredCapacity, localIndices.length * 2);
      localIndices = java.util.Arrays.copyOf(localIndices, newCapacity);
      stateBits = java.util.Arrays.copyOf(stateBits, newCapacity);
    }
  }

  private record ScheduledTick(BlockPos pos, int delayTicks, int priority) {}

  private record NeighborNotification(BlockPos pos, BlockPos source) {}

  /** Backing world interface used by the overlay. */
  public interface Delegate {
    int getBlockStateBits(BlockPos pos);

    void setBlockStateBits(BlockPos pos, int stateBits);

    void scheduleTick(BlockPos pos, int delayTicks, int priority);

    void markNeighborChanged(BlockPos pos, BlockPos source);
  }
}
