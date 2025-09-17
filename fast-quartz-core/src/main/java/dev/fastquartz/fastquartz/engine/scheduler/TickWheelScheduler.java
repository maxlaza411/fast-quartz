package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Two-level scheduler backed by a hashed timing wheel and a deterministic micro-priority queue. */
public final class TickWheelScheduler implements Scheduler {
  private static final int DEFAULT_WHEEL_SIZE = 4096;

  private final ArrayDeque<Event>[] wheel;
  private final MicroPriorityQueue microQueue;
  private final int mask;
  private final SchedulerMetrics metrics = new SchedulerMetrics();

  public TickWheelScheduler() {
    this(DEFAULT_WHEEL_SIZE);
  }

  @SuppressWarnings("unchecked")
  public TickWheelScheduler(int wheelSize) {
    if (wheelSize <= 0 || (wheelSize & (wheelSize - 1)) != 0) {
      throw new IllegalArgumentException("wheelSize must be a positive power-of-two");
    }
    this.wheel = (ArrayDeque<Event>[]) new ArrayDeque<?>[wheelSize];
    for (int i = 0; i < wheelSize; i++) {
      this.wheel[i] = new ArrayDeque<>();
    }
    this.microQueue = new MicroPriorityQueue();
    this.mask = wheelSize - 1;
  }

  @Override
  public void scheduleAtTick(Event event, long tickDue) {
    Objects.requireNonNull(event, "event");
    if (event.key().tick() != tickDue) {
      throw new IllegalArgumentException("event key tick mismatch");
    }
    long start = this.metrics.startTimer();
    try {
      this.wheel[indexFor(tickDue)].addLast(event);
    } finally {
      this.metrics.recordScheduleAtTick(start);
    }
  }

  @Override
  public void scheduleMicro(Event event, int microDue) {
    Objects.requireNonNull(event, "event");
    if (event.key().micro() != microDue) {
      throw new IllegalArgumentException("event key micro mismatch");
    }
    long start = this.metrics.startTimer();
    try {
      this.microQueue.offer(event);
    } finally {
      this.metrics.recordScheduleMicro(start);
    }
  }

  @Override
  public void cancelAt(BlockPos pos, EvType type) {
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(type, "type");
    long start = this.metrics.startTimer();
    try {
      this.microQueue.remove(pos, type);
      for (ArrayDeque<Event> slot : this.wheel) {
        if (slot.isEmpty()) {
          continue;
        }
        int iterations = slot.size();
        for (int i = 0; i < iterations; i++) {
          Event event = slot.removeFirst();
          if (event.pos().equals(pos) && event.type() == type) {
            continue;
          }
          slot.addLast(event);
        }
      }
    } finally {
      this.metrics.recordCancel(start);
    }
  }

  @Override
  public boolean hasDueAt(long tick) {
    ArrayDeque<Event> slot = this.wheel[indexFor(tick)];
    if (slot.isEmpty()) {
      return false;
    }
    for (Event event : slot) {
      if (event.key().tick() <= tick) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<Event> drainTick(long tick) {
    long start = this.metrics.startTimer();
    try {
      ArrayDeque<Event> slot = this.wheel[indexFor(tick)];
      List<Event> drained = new ArrayList<>(slot.size());
      if (slot.isEmpty()) {
        return drained;
      }
      int iterations = slot.size();
      for (int i = 0; i < iterations; i++) {
        Event event = slot.removeFirst();
        if (event.key().tick() <= tick) {
          this.microQueue.offer(event);
          drained.add(event);
        } else {
          slot.addLast(event);
        }
      }
      return drained;
    } finally {
      this.metrics.recordDrainTick(start);
    }
  }

  @Override
  public @Nullable Event pollMicro() {
    long start = this.metrics.startTimer();
    try {
      return this.microQueue.poll();
    } finally {
      this.metrics.recordPollMicro(start);
    }
  }

  @Override
  public void offerMicro(Event event) {
    Objects.requireNonNull(event, "event");
    long start = this.metrics.startTimer();
    try {
      this.microQueue.offer(event);
    } finally {
      this.metrics.recordOfferMicro(start);
    }
  }

  @Override
  public SchedulerMetrics metrics() {
    return this.metrics;
  }

  private int indexFor(long tick) {
    return (int) (tick & this.mask);
  }
}
