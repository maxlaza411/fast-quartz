package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  private static EventKey randomKey(ThreadLocalRandom random) {
    long tick = random.nextLong(0L, 1_000_000L);
    int micro = random.nextInt(0, 1_024);
    int region = random.nextInt(0, 64);
    long local = random.nextLong();
    return new EventKey(tick, micro, region, local);
  }
}
