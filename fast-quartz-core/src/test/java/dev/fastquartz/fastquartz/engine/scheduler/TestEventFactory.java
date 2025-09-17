package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;

final class TestEventFactory {
  private TestEventFactory() {}

  static Event create(long tick, int micro, int regionId, BlockPos pos, EvType type) {
    return create(tick, micro, regionId, pos, type, 0, null);
  }

  static Event create(
      long tick, int micro, int regionId, BlockPos pos, EvType type, int aux, Object data) {
    EventKey key = new EventKey(tick, micro, regionId, EventKey.localOrder(pos, type));
    return new Event(key, type, pos, aux, data);
  }
}
