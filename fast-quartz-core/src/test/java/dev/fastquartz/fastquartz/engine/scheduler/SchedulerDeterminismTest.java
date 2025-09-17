package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SchedulerDeterminismTest {
  private static final int EVENT_COUNT = 512;

  @Test
  void dequeueOrderIsStableAcrossRuns() {
    List<Event> events = randomEvents(0x5EEDL);
    List<EventKey> firstRun = drainInOrder(events);
    List<EventKey> secondRun = drainInOrder(events);
    assertEquals(firstRun, secondRun, "Scheduling order must be deterministic");
  }

  private static List<Event> randomEvents(long seed) {
    Random random = new Random(seed);
    EvType[] types = EvType.values();
    List<Event> events = new ArrayList<>(EVENT_COUNT);
    for (int i = 0; i < EVENT_COUNT; i++) {
      long tick = random.nextInt(2_048);
      int micro = random.nextInt(64);
      int region = random.nextInt(16);
      BlockPos pos = new BlockPos(random.nextInt(64) - 32, random.nextInt(32), random.nextInt(64) - 32);
      EvType type = types[random.nextInt(types.length)];
      events.add(TestEventFactory.create(tick, micro, region, pos, type));
    }
    return events;
  }

  private static List<EventKey> drainInOrder(List<Event> events) {
    TickWheelScheduler scheduler = new TickWheelScheduler(64);
    long maxTick = 0L;
    for (Event event : events) {
      scheduler.scheduleAtTick(event, event.key().tick());
      maxTick = Math.max(maxTick, event.key().tick());
    }
    List<EventKey> observed = new ArrayList<>(events.size());
    for (long tick = 0; tick <= maxTick; tick++) {
      scheduler.drainTick(tick);
      Event next;
      while ((next = scheduler.pollMicro()) != null) {
        assertTrue(next.key().tick() <= tick, "Events must not fire before their tick");
        observed.add(next.key());
      }
    }
    assertEquals(events.size(), observed.size());
    return observed;
  }
}
