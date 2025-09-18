package dev.fastquartz.engine.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventQueueTest {
  @Test
  void eventsDequeueInLexicographicOrder() {
    EventQueue<String> queue = new EventQueue<>();
    List<EventKey> keys =
        List.of(
            EventKey.forBlock(1, 0, 0, 0, 0, 0, EventType.SCHEDULED),
            EventKey.forBlock(1, 0, 0, 1, 0, 0, EventType.SCHEDULED),
            EventKey.forBlock(4, 1, 0, 2, 0, 0, EventType.LOCAL_UPDATE),
            EventKey.forBlock(8, 2, 1, 3, 1, 1, EventType.STATEFUL),
            EventKey.forBlock(8, 3, 2, 3, 1, 1, EventType.STATEFUL));
    List<EventKey> shuffled = new ArrayList<>(keys);
    Collections.shuffle(shuffled);
    for (int i = 0; i < shuffled.size(); i++) {
      queue.schedule(shuffled.get(i), "event-" + i);
    }

    List<EventKey> drained = new ArrayList<>();
    EventQueue.Event<String> event;
    while ((event = queue.poll()) != null) {
      drained.add(event.key());
    }

    assertEquals(keys, drained);
  }

  @Test
  void identicalKeysPreserveInsertionOrder() {
    EventQueue<String> queue = new EventQueue<>();
    EventKey key = EventKey.forBlock(5, 4, 3, 2, 1, 0, EventType.POWER_PROP);
    queue.schedule(key, "first");
    queue.schedule(key, "second");

    assertEquals("first", queue.poll().payload());
    assertEquals("second", queue.poll().payload());
    assertNull(queue.poll());
  }

  @Test
  void timingWheelHandlesWideGaps() {
    EventQueue<String> queue = new EventQueue<>();
    long[] ticks = {3, 20, 300, 9_000, 120_000, 5_000_000, 120_000_000L};
    for (long tick : ticks) {
      queue.schedule(EventKey.forBlock(tick, 0, 0, 1, 0, 0, EventType.SCHEDULED), "tick-" + tick);
    }

    List<Long> drainedTicks = new ArrayList<>();
    EventQueue.Event<String> event;
    while ((event = queue.poll()) != null) {
      drainedTicks.add(event.key().tick());
    }

    List<Long> expected = Arrays.stream(ticks).boxed().sorted().toList();
    assertEquals(expected, drainedTicks);
  }

  @Test
  void scheduleDuringActiveTickRunsWithinSameTick() {
    EventQueue<String> queue = new EventQueue<>();
    EventKey first = EventKey.forBlock(10, 0, 0, 0, 0, 0, EventType.SCHEDULED);
    EventKey second = EventKey.forBlock(10, 5, 0, 1, 0, 0, EventType.LOCAL_UPDATE);

    queue.schedule(first, "first");
    EventQueue.Event<String> firstEvent = queue.poll();
    assertEquals(first, firstEvent.key());

    queue.schedule(second, "second");
    EventQueue.Event<String> secondEvent = queue.poll();
    assertEquals(second, secondEvent.key());
    assertEquals(first.tick(), secondEvent.key().tick());
  }

  @Test
  void schedulingEventsInThePastIsRejected() {
    EventQueue<String> queue = new EventQueue<>();
    EventKey baseline = EventKey.forBlock(4, 0, 0, 0, 0, 0, EventType.SCHEDULED);
    queue.schedule(baseline, "baseline");
    assertEquals(baseline, queue.poll().key());

    assertThrows(
        IllegalArgumentException.class,
        () -> queue.schedule(EventKey.forBlock(3, 0, 0, 0, 0, 0, EventType.SCHEDULED), "late"));
  }

  @Test
  void emptyQueueReportsAsEmpty() {
    EventQueue<String> queue = new EventQueue<>();
    assertTrue(queue.isEmpty());
    queue.schedule(EventKey.forBlock(0, 0, 0, 0, 0, 0, EventType.SCHEDULED), "tick0");
    assertFalse(queue.isEmpty());
    queue.poll();
    assertNull(queue.poll());
    assertTrue(queue.isEmpty());
  }
}
