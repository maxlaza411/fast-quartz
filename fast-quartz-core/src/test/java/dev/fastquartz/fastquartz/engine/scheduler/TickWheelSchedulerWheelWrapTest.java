package dev.fastquartz.fastquartz.engine.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TickWheelSchedulerWheelWrapTest {
  @Test
  void eventsBeyondWheelSizeFireOnCorrectTick() {
    int wheelSize = 8;
    TickWheelScheduler scheduler = new TickWheelScheduler(wheelSize);
    List<Event> events = List.of(
        TestEventFactory.create(1, 0, 0, new BlockPos(1, 0, 0), EvType.BLOCK_SCHEDULED_TICK),
        TestEventFactory.create(9, 0, 0, new BlockPos(2, 0, 0), EvType.NEIGHBOR_CHANGE),
        TestEventFactory.create(17, 0, 0, new BlockPos(3, 0, 0), EvType.POWER_PROPAGATION));

    long maxTick = 0L;
    for (Event event : events) {
      scheduler.scheduleAtTick(event, event.key().tick());
      maxTick = Math.max(maxTick, event.key().tick());
    }

    Map<Long, List<Event>> fired = new HashMap<>();
    for (long tick = 0; tick <= maxTick; tick++) {
      scheduler.drainTick(tick);
      Event next;
      while ((next = scheduler.pollMicro()) != null) {
        fired.computeIfAbsent(tick, ignored -> new ArrayList<>()).add(next);
        assertTrue(next.key().tick() <= tick, "Event fired before scheduled tick");
      }
    }

    assertTrue(fired.containsKey(1L));
    assertTrue(fired.containsKey(9L));
    assertTrue(fired.containsKey(17L));
    assertEquals(1, fired.get(1L).size());
    assertEquals(1, fired.get(9L).size());
    assertEquals(1, fired.get(17L).size());
    assertFalse(scheduler.hasDueAt(18L));
  }
}
