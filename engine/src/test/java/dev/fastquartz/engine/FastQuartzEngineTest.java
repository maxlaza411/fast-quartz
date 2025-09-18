package dev.fastquartz.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class FastQuartzEngineTest {
  @Test
  void createRejectsNonPositiveTps() {
    assertThrows(IllegalArgumentException.class, () -> FastQuartzEngine.create(0));
    assertThrows(IllegalArgumentException.class, () -> FastQuartzEngine.create(-20));
  }

  @Test
  void tickDurationMatchesTargetTps() {
    FastQuartzEngine engine = FastQuartzEngine.create(20);
    assertEquals(Duration.ofMillis(50), engine.tickDuration());
  }

  @Test
  void ticksForDurationRoundsToNearestTick() {
    FastQuartzEngine engine = FastQuartzEngine.create(20);
    assertEquals(2, engine.ticksFor(Duration.ofMillis(120))); // 2.4 -> rounds to 2
    assertEquals(3, engine.ticksFor(Duration.ofMillis(160))); // 3.2 -> rounds to 3
  }
}
