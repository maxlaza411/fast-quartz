package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.Arrays;

/** Binary heap implementation specialised for {@link Event} ordering. */
final class MicroPriorityQueue {
  private static final int DEFAULT_CAPACITY = 64;

  private Event[] heap;
  private int size;

  MicroPriorityQueue() {
    this(DEFAULT_CAPACITY);
  }

  MicroPriorityQueue(int initialCapacity) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("initialCapacity must be positive");
    }
    this.heap = new Event[initialCapacity];
    this.size = 0;
  }

  void offer(Event event) {
    ensureCapacity();
    this.heap[this.size] = event;
    siftUp(this.size);
    this.size++;
  }

  @Nullable
  Event poll() {
    if (this.size == 0) {
      return null;
    }
    Event result = this.heap[0];
    int newSize = --this.size;
    Event last = this.heap[newSize];
    this.heap[newSize] = null;
    if (newSize > 0) {
      this.heap[0] = last;
      siftDown(0);
    }
    return result;
  }

  boolean isEmpty() {
    return this.size == 0;
  }

  int size() {
    return this.size;
  }

  int remove(BlockPos pos, EvType type) {
    int removed = 0;
    int index = 0;
    while (index < this.size) {
      Event event = this.heap[index];
      if (event != null && event.pos().equals(pos) && event.type() == type) {
        removed++;
        removeAt(index);
      } else {
        index++;
      }
    }
    return removed;
  }

  private void removeAt(int index) {
    int newSize = --this.size;
    Event last = this.heap[newSize];
    this.heap[newSize] = null;
    if (index == newSize) {
      return;
    }
    this.heap[index] = last;
    siftDown(index);
    siftUp(index);
  }

  private void ensureCapacity() {
    if (this.size < this.heap.length) {
      return;
    }
    int newCapacity = this.heap.length + (this.heap.length >> 1);
    if (newCapacity == this.heap.length) {
      newCapacity++;
    }
    this.heap = Arrays.copyOf(this.heap, newCapacity);
  }

  private void siftUp(int index) {
    Event event = this.heap[index];
    while (index > 0) {
      int parent = (index - 1) >>> 1;
      Event parentEvent = this.heap[parent];
      if (compare(event, parentEvent) >= 0) {
        break;
      }
      this.heap[index] = parentEvent;
      index = parent;
    }
    this.heap[index] = event;
  }

  private void siftDown(int index) {
    Event event = this.heap[index];
    int half = this.size >>> 1;
    while (index < half) {
      int left = (index << 1) + 1;
      int right = left + 1;
      int smallest = left;
      Event leftEvent = this.heap[left];
      if (right < this.size && compare(this.heap[right], leftEvent) < 0) {
        smallest = right;
      }
      Event smallestEvent = this.heap[smallest];
      if (compare(event, smallestEvent) <= 0) {
        break;
      }
      this.heap[index] = smallestEvent;
      index = smallest;
    }
    this.heap[index] = event;
  }

  private static int compare(Event first, Event second) {
    return first.key().compareTo(second.key());
  }
}
