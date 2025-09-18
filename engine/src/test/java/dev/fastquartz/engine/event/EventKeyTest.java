package dev.fastquartz.engine.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventKeyTest {
  @Test
  void packLocalOrderCombinesCoordinatesAndTypeOrdinal() {
    long packed = EventKey.packLocalOrder(12, 34, 56, EventType.OBSERVER_EMIT);
    long expected =
        ((long) 34 << 40)
            | ((long) 56 << 20)
            | ((long) 12 << 4)
            | EventType.OBSERVER_EMIT.eventOrdinal();
    assertEquals(expected, packed);
  }

  @Test
  void packLocalOrderRejectsOutOfRangeCoordinates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> EventKey.packLocalOrder(-1, 0, 0, EventType.SCHEDULED));
    assertThrows(
        IllegalArgumentException.class,
        () -> EventKey.packLocalOrder(0, 1 << 20, 0, EventType.SCHEDULED));
  }

  @Test
  void compareToOrdersLexicographically() {
    EventKey a = EventKey.forBlock(0, 0, 0, 1, 1, 1, EventType.SCHEDULED);
    EventKey b = EventKey.forBlock(0, 1, 0, 1, 1, 1, EventType.SCHEDULED);
    EventKey c = EventKey.forBlock(0, 1, 1, 1, 1, 1, EventType.SCHEDULED);
    EventKey d = EventKey.forBlock(0, 1, 1, 1, 1, 2, EventType.LOCAL_UPDATE);
    EventKey e = EventKey.forBlock(1, 0, 0, 1, 1, 1, EventType.SCHEDULED);

    List<EventKey> keys = new ArrayList<>(List.of(e, d, c, b, a));
    Collections.shuffle(keys);
    Collections.sort(keys);

    assertEquals(List.of(a, b, c, d, e), keys);
  }
}
