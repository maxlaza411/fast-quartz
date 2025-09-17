package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerLatencyTest {
  private static final int EVENT_COUNT = 10_000;
  private static final double BUDGET_MICROS = 200.0d;

  @Test
  void enqueueAndDequeueStayWithinLatencyBudget() {
    TickWheelScheduler scheduler = new TickWheelScheduler();
    List<Event> events = new ArrayList<>(EVENT_COUNT);
    EvType[] types = EvType.values();
    for (int i = 0; i < EVENT_COUNT; i++) {
      long tick = i;
      int micro = i & 0x0F;
      int region = i & 0x07;
      BlockPos pos = new BlockPos(i & 0x3F, (i >> 3) & 0x3F, (i >> 6) & 0x3F);
      EvType type = types[i % types.length];
      events.add(TestEventFactory.create(tick, micro, region, pos, type));
    }

    long scheduleStart = System.nanoTime();
    for (Event event : events) {
      scheduler.scheduleAtTick(event, event.key().tick());
    }
    long scheduleDuration = System.nanoTime() - scheduleStart;

    long drainStart = System.nanoTime();
    for (long tick = 0; tick < EVENT_COUNT; tick++) {
      scheduler.drainTick(tick);
    }
    long drainDuration = System.nanoTime() - drainStart;

    long pollStart = System.nanoTime();
    for (int i = 0; i < EVENT_COUNT; i++) {
      assertTrue(scheduler.pollMicro() != null);
    }
    long pollDuration = System.nanoTime() - pollStart;

    assertTrue(scheduler.pollMicro() == null, "Queue should be empty after draining all events");

    double scheduleAverage = nanosToMicros(scheduleDuration) / EVENT_COUNT;
    double drainAverage = nanosToMicros(drainDuration) / EVENT_COUNT;
    double pollAverage = nanosToMicros(pollDuration) / EVENT_COUNT;

    assertTrue(scheduleAverage < BUDGET_MICROS, "scheduleAtTick should stay within budget");
    assertTrue(drainAverage < BUDGET_MICROS, "drainTick should stay within budget");
    assertTrue(pollAverage < BUDGET_MICROS, "pollMicro should stay within budget");

    SchedulerMetrics metrics = scheduler.metrics();
    assertEquals(EVENT_COUNT, metrics.scheduleAtTickOps());
    assertEquals(EVENT_COUNT, metrics.drainTickOps());
    assertEquals(EVENT_COUNT + 1L, metrics.pollMicroOps());
    assertTrue(metrics.scheduleAtTickAverageMicros() < BUDGET_MICROS);
    assertTrue(metrics.drainTickAverageMicros() < BUDGET_MICROS);
    assertTrue(metrics.pollMicroAverageMicros() < BUDGET_MICROS);
  }

  private static double nanosToMicros(long nanos) {
    return nanos / 1_000.0d;
  }
}
