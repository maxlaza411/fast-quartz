package dev.fastquartz.engine.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Deterministic event queue with a hierarchical timing wheel. Events are ordered lexicographically
 * by {@link EventKey}. Within identical keys, insertion order is preserved.
 */
public final class EventQueue<T> {
  private final Queue<ScheduledEvent<T>> readyQueue = new PriorityQueue<>();
  private final TimingWheel<T> timingWheel;
  private long sequenceCounter;
  private long activeTick = Long.MIN_VALUE;

  /** Creates a queue starting at tick zero. */
  public EventQueue() {
    this(0L);
  }

  public EventQueue(long startTick) {
    if (startTick < 0) {
      throw new IllegalArgumentException("startTick must be non-negative");
    }
    this.timingWheel = new TimingWheel<>(startTick);
  }

  /** Schedules an event for delivery based on its key. */
  public void schedule(EventKey key, T payload) {
    Objects.requireNonNull(key, "key");
    ScheduledEvent<T> event = new ScheduledEvent<>(key, payload, sequenceCounter++);
    if (activeTick != Long.MIN_VALUE && key.tick() < activeTick) {
      throw new IllegalArgumentException("Cannot schedule event in the past: " + key);
    }
    if (activeTick != Long.MIN_VALUE && key.tick() == activeTick) {
      readyQueue.add(event);
    } else {
      timingWheel.schedule(event);
    }
  }

  /** Returns {@code true} if no events remain either in the ready queue or timing wheel. */
  public boolean isEmpty() {
    return readyQueue.isEmpty() && timingWheel.isEmpty();
  }

  /** Clears all pending events and rewinds the timing wheel to its start tick. */
  public void clear() {
    readyQueue.clear();
    timingWheel.clear();
    activeTick = Long.MIN_VALUE;
    sequenceCounter = 0L;
  }

  /** Retrieves and removes the next event according to the deterministic ordering. */
  public Event<T> poll() {
    if (!ensureReady()) {
      return null;
    }
    ScheduledEvent<T> scheduled = readyQueue.poll();
    if (scheduled == null) {
      return null;
    }
    activeTick = scheduled.key().tick();
    return scheduled.toEvent();
  }

  private boolean ensureReady() {
    if (!readyQueue.isEmpty()) {
      return true;
    }
    while (readyQueue.isEmpty()) {
      long nextTick = timingWheel.nextTick();
      if (nextTick == Long.MAX_VALUE) {
        activeTick = Long.MIN_VALUE;
        return false;
      }
      if (activeTick != Long.MIN_VALUE && nextTick < activeTick) {
        throw new IllegalStateException(
            "Timing wheel produced non-monotonic tick: " + nextTick + " < " + activeTick);
      }
      timingWheel.collectTick(nextTick, readyQueue::add);
      activeTick = nextTick;
    }
    return true;
  }

  /** Immutable view of a dequeued event. */
  public static final class Event<T> {
    private final EventKey key;
    private final T payload;

    public Event(EventKey key, T payload) {
      this.key = Objects.requireNonNull(key, "key");
      this.payload = payload;
    }

    public EventKey key() {
      return key;
    }

    public T payload() {
      return payload;
    }
  }

  private static final class ScheduledEvent<T> implements Comparable<ScheduledEvent<T>> {
    private final EventKey key;
    private final T payload;
    private final long sequence;

    ScheduledEvent(EventKey key, T payload, long sequence) {
      this.key = Objects.requireNonNull(key, "key");
      this.payload = payload;
      this.sequence = sequence;
    }

    Event<T> toEvent() {
      return new Event<>(key, payload);
    }

    EventKey key() {
      return key;
    }

    @Override
    public int compareTo(ScheduledEvent<T> other) {
      int cmp = key.compareTo(other.key);
      if (cmp != 0) {
        return cmp;
      }
      return Long.compare(sequence, other.sequence);
    }
  }

  private static final class TimingWheel<T> {
    private static final int LEVEL_BITS = 4;
    private static final int WHEEL_SIZE = 1 << LEVEL_BITS; // 16 slots per tier.
    private static final int LEVEL_COUNT = 8;
    private static final int WHEEL_MASK = WHEEL_SIZE - 1;

    private final List<List<ArrayDeque<ScheduledEvent<T>>>> levels = new ArrayList<>(LEVEL_COUNT);
    private final long startTick;
    private long cursor;

    TimingWheel(long startTick) {
      this.startTick = startTick;
      this.cursor = startTick;
      for (int level = 0; level < LEVEL_COUNT; level++) {
        ArrayList<ArrayDeque<ScheduledEvent<T>>> slots = new ArrayList<>(WHEEL_SIZE);
        for (int slot = 0; slot < WHEEL_SIZE; slot++) {
          slots.add(new ArrayDeque<>());
        }
        levels.add(slots);
      }
    }

    void schedule(ScheduledEvent<T> event) {
      long tick = event.key().tick();
      if (tick < cursor) {
        throw new IllegalArgumentException("tick " + tick + " < cursor " + cursor);
      }
      int level = selectLevel(tick);
      enqueue(level, event);
    }

    boolean isEmpty() {
      for (List<ArrayDeque<ScheduledEvent<T>>> level : levels) {
        for (ArrayDeque<ScheduledEvent<T>> bucket : level) {
          if (!bucket.isEmpty()) {
            return false;
          }
        }
      }
      return true;
    }

    void clear() {
      for (List<ArrayDeque<ScheduledEvent<T>>> level : levels) {
        for (ArrayDeque<ScheduledEvent<T>> bucket : level) {
          bucket.clear();
        }
      }
      cursor = startTick;
    }

    long nextTick() {
      long min = Long.MAX_VALUE;
      for (List<ArrayDeque<ScheduledEvent<T>>> level : levels) {
        for (ArrayDeque<ScheduledEvent<T>> bucket : level) {
          if (!bucket.isEmpty()) {
            for (ScheduledEvent<T> event : bucket) {
              long tick = event.key().tick();
              if (tick < min) {
                min = tick;
              }
            }
          }
        }
      }
      return min;
    }

    void collectTick(long tick, Consumer<ScheduledEvent<T>> consumer) {
      if (tick < cursor) {
        throw new IllegalArgumentException(
            "Cannot collect past tick " + tick + " when cursor=" + cursor);
      }
      cursor = tick;
      for (int level = LEVEL_COUNT - 1; level >= 1; level--) {
        int currentLevel = level;
        drainLevel(currentLevel, tick, event -> enqueue(currentLevel - 1, event));
      }
      drainLevel(0, tick, consumer);
    }

    private void drainLevel(int level, long tick, Consumer<ScheduledEvent<T>> consumer) {
      ArrayDeque<ScheduledEvent<T>> bucket = bucket(level, tick);
      int initialSize = bucket.size();
      for (int i = 0; i < initialSize; i++) {
        ScheduledEvent<T> event = bucket.pollFirst();
        if (event == null) {
          break;
        }
        if (event.key().tick() == tick) {
          consumer.accept(event);
        } else {
          bucket.addLast(event);
        }
      }
    }

    private void enqueue(int level, ScheduledEvent<T> event) {
      bucket(level, event.key().tick()).addLast(event);
    }

    private ArrayDeque<ScheduledEvent<T>> bucket(int level, long tick) {
      int slot = (int) ((tick >> (level * LEVEL_BITS)) & WHEEL_MASK);
      return levels.get(level).get(slot);
    }

    private int selectLevel(long tick) {
      long relative = tick - cursor;
      int level = 0;
      while (level < LEVEL_COUNT - 1) {
        long span = 1L << ((level + 1) * LEVEL_BITS);
        if (relative < span) {
          break;
        }
        level++;
      }
      return level;
    }
  }
}
