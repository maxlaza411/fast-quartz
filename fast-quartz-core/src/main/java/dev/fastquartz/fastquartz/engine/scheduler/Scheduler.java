package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.List;

/** API for the two-level event scheduler. */
public interface Scheduler {
  void scheduleAtTick(Event event, long tickDue);

  void scheduleMicro(Event event, int microDue);

  void cancelAt(BlockPos pos, EvType type);

  boolean hasDueAt(long tick);

  List<Event> drainTick(long tick);

  @Nullable
  Event pollMicro();

  void offerMicro(Event event);

  SchedulerMetrics metrics();
}
