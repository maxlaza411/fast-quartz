package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.fastquartz.fastquartz.engine.BlockPos;
import org.junit.jupiter.api.Test;

class SchedulerMicroOrderingTest {
  @Test
  void scheduleMicroRespectsOrdering() {
    TickWheelScheduler scheduler = new TickWheelScheduler();
    Event first =
        TestEventFactory.create(42, 1, 0, new BlockPos(0, 0, 0), EvType.COMPONENT_EVAL);
    Event second =
        TestEventFactory.create(42, 5, 0, new BlockPos(1, 0, 0), EvType.COMPONENT_EVAL);
    Event third =
        TestEventFactory.create(42, 9, 0, new BlockPos(2, 0, 0), EvType.COMPONENT_EVAL);

    scheduler.scheduleMicro(second, second.key().micro());
    scheduler.scheduleMicro(third, third.key().micro());
    scheduler.scheduleMicro(first, first.key().micro());

    assertEquals(first.key(), scheduler.pollMicro().key());
    assertEquals(second.key(), scheduler.pollMicro().key());
    assertEquals(third.key(), scheduler.pollMicro().key());
  }
}
