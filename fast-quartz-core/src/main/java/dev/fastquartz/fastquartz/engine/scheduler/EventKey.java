package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;

/** Stable ordering key for scheduled events. */
public record EventKey(long tick, int micro, int regionId, long localOrder)
    implements Comparable<EventKey> {

  private static final long COORD_MASK = (1L << 20) - 1L;

  @Override
  public int compareTo(EventKey other) {
    if (this == other) {
      return 0;
    }
    int tickCompare = Long.compare(this.tick, other.tick);
    if (tickCompare != 0) {
      return tickCompare;
    }
    int microCompare = Integer.compare(this.micro, other.micro);
    if (microCompare != 0) {
      return microCompare;
    }
    int regionCompare = Integer.compare(this.regionId, other.regionId);
    if (regionCompare != 0) {
      return regionCompare;
    }
    return Long.compare(this.localOrder, other.localOrder);
  }

  /**
   * Computes a deterministic intra-tick ordering value using Y-major packing of the block position
   * along with the event type ordinal.
   */
  @SuppressWarnings("EnumOrdinal")
  public static long localOrder(BlockPos pos, EvType type) {
    long yBits = compactCoordinate(pos.y());
    long zBits = compactCoordinate(pos.z());
    long xBits = compactCoordinate(pos.x());
    return (yBits << 40) | (zBits << 20) | (xBits << 4) | (long) type.ordinal();
  }

  private static long compactCoordinate(int value) {
    return ((long) value) & COORD_MASK;
  }
}
