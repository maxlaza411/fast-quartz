package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;

/** Stable ordering key for scheduled events. */
public record EventKey(long tick, int micro, int regionId, long localOrder)
    implements Comparable<EventKey> {

  private static final int AXIS_BITS = 20;
  private static final int TYPE_BITS = 4;
  private static final long AXIS_MASK = (1L << AXIS_BITS) - 1L;
  private static final long AXIS_OFFSET = 1L << (AXIS_BITS - 1);

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
    return Long.compareUnsigned(this.localOrder, other.localOrder);
  }

  /**
   * Computes a deterministic intra-tick ordering value using Y-major packing of the block position
   * along with the event type ordinal. Coordinates are normalised into a 20-bit signed range so that
   * the resulting {@code long} can be compared using unsigned semantics while preserving the
   * lexicographic (Y, Z, X, type) ordering requirements.
   */
  @SuppressWarnings("EnumOrdinal")
  public static long localOrder(BlockPos pos, EvType type) {
    long yBits = compactCoordinate(pos.y());
    long zBits = compactCoordinate(pos.z());
    long xBits = compactCoordinate(pos.x());
    return (yBits << (AXIS_BITS * 2 + TYPE_BITS))
        | (zBits << (AXIS_BITS + TYPE_BITS))
        | (xBits << TYPE_BITS)
        | (long) type.ordinal();
  }

  private static long compactCoordinate(int value) {
    long adjusted = (long) value + AXIS_OFFSET;
    if (adjusted < 0L || adjusted > AXIS_MASK) {
      throw new IllegalArgumentException("Coordinate out of supported range: " + value);
    }
    return adjusted;
  }
}
