package dev.fastquartz.engine.event;

import java.util.Objects;

/** Lexicographically ordered event key: (tick, micro, region, local order). */
public record EventKey(long tick, int micro, int regionId, long localOrder)
    implements Comparable<EventKey> {
  private static final int MICRO_MIN = 0;
  private static final int MICRO_MAX = 9;

  private static final int EVENT_TYPE_BITS = 4;
  private static final int X_BITS = 16;
  private static final int Z_BITS = 20;
  private static final int Y_BITS = 20;
  private static final int X_SHIFT = EVENT_TYPE_BITS;
  private static final int Z_SHIFT = X_SHIFT + X_BITS; // matches (x << 4) / (z << 20)
  private static final int Y_SHIFT = Z_SHIFT + Z_BITS;
  private static final long X_MASK = (1L << X_BITS) - 1;
  private static final long Z_MASK = (1L << Z_BITS) - 1;
  private static final long Y_MASK = (1L << Y_BITS) - 1;
  private static final long EVENT_MASK = (1L << EVENT_TYPE_BITS) - 1;

  public EventKey {
    if (tick < 0) {
      throw new IllegalArgumentException("tick must be non-negative");
    }
    if (micro < MICRO_MIN || micro > MICRO_MAX) {
      throw new IllegalArgumentException("micro out of range: " + micro);
    }
    if (localOrder < 0) {
      throw new IllegalArgumentException("localOrder must be non-negative");
    }
  }

  /** Convenience factory mirroring the canonical record constructor. */
  public static EventKey of(long tick, int micro, int regionId, long localOrder) {
    return new EventKey(tick, micro, regionId, localOrder);
  }

  /**
   * Packs block coordinates and the locked event ordinal into the {@code localOrder} lane. The
   * layout matches the spec: {@code (y << 40) | (z << 20) | (x << 4) | eventTypeOrdinal}.
   */
  public static long packLocalOrder(int x, int y, int z, EventType type) {
    Objects.requireNonNull(type, "type");
    checkCoordinate("x", x, X_MASK);
    checkCoordinate("y", y, Y_MASK);
    checkCoordinate("z", z, Z_MASK);
    int ordinal = type.eventOrdinal();
    if ((ordinal & ~EVENT_MASK) != 0) {
      throw new IllegalArgumentException("event ordinal does not fit: " + ordinal);
    }
    return (((long) y & Y_MASK) << Y_SHIFT)
        | (((long) z & Z_MASK) << Z_SHIFT)
        | (((long) x & X_MASK) << X_SHIFT)
        | ordinal;
  }

  /** Creates an event key by deriving the local order from block coordinates and event type. */
  public static EventKey forBlock(
      long tick, int micro, int regionId, int x, int y, int z, EventType type) {
    return new EventKey(tick, micro, regionId, packLocalOrder(x, y, z, type));
  }

  private static void checkCoordinate(String axis, int value, long mask) {
    if (value < 0 || (((long) value) & ~mask) != 0) {
      throw new IllegalArgumentException(axis + " coordinate out of range: " + value);
    }
  }

  @Override
  public int compareTo(EventKey other) {
    Objects.requireNonNull(other, "other");
    int cmp = Long.compare(tick, other.tick);
    if (cmp != 0) {
      return cmp;
    }
    cmp = Integer.compare(micro, other.micro);
    if (cmp != 0) {
      return cmp;
    }
    cmp = Integer.compare(regionId, other.regionId);
    if (cmp != 0) {
      return cmp;
    }
    return Long.compareUnsigned(localOrder, other.localOrder);
  }
}
