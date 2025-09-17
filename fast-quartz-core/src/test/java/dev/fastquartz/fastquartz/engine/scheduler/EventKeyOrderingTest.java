package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class EventKeyOrderingTest {
  private static final int SAMPLE_SIZE = 100_000;

  @Test
  void compareToIsAntisymmetric() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      EventKey first = randomKey(random);
      EventKey second = randomKey(random);
      int forward = Integer.signum(first.compareTo(second));
      int backward = Integer.signum(second.compareTo(first));
      assertEquals(-forward, backward, "Ordering must be antisymmetric");
    }
  }

  @Test
  void compareToIsTransitive() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      EventKey a = randomKey(random);
      EventKey b = randomKey(random);
      EventKey c = randomKey(random);
      if (a.compareTo(b) <= 0 && b.compareTo(c) <= 0) {
        assertTrue(a.compareTo(c) <= 0, "Ordering must be transitive (ascending)");
      }
      if (a.compareTo(b) >= 0 && b.compareTo(c) >= 0) {
        assertTrue(a.compareTo(c) >= 0, "Ordering must be transitive (descending)");
      }
    }
  }

  @Test
  void localOrderRespectsBlockPosOrdering() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      BlockPos first = randomPos(random);
      BlockPos second = randomPos(random);
      long firstOrder = EventKey.localOrder(first, EvType.BLOCK_SCHEDULED_TICK);
      long secondOrder = EventKey.localOrder(second, EvType.BLOCK_SCHEDULED_TICK);
      int expected = Integer.signum(first.compareTo(second));
      int actual = Integer.signum(Long.compareUnsigned(firstOrder, secondOrder));
      assertEquals(expected, Integer.signum(actual), "localOrder must follow BlockPos ordering");
    }
  }

  @Test
  void localOrderBreaksTiesUsingEventType() {
    BlockPos pos = new BlockPos(4, 2, -3);
    EvType[] types = EvType.values();
    long previous = EventKey.localOrder(pos, types[0]);
    for (int i = 1; i < types.length; i++) {
      long encoded = EventKey.localOrder(pos, types[i]);
      assertTrue(
          Long.compareUnsigned(previous, encoded) < 0,
          "Type ordinal should strictly increase local order");
      previous = encoded;
    }
  }

  private static EventKey randomKey(ThreadLocalRandom random) {
    long tick = random.nextLong(0L, 1_000_000L);
    int micro = random.nextInt(0, 1_024);
    int region = random.nextInt(0, 64);
    long local = random.nextLong();
    return new EventKey(tick, micro, region, local);
  }

  private static BlockPos randomPos(ThreadLocalRandom random) {
    int limit = 1 << 19;
    int x = random.nextInt(-limit, limit);
    int y = random.nextInt(-limit, limit);
    int z = random.nextInt(-limit, limit);
    return new BlockPos(x, y, z);
  }
}
