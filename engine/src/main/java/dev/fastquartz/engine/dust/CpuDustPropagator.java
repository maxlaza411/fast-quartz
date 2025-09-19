package dev.fastquartz.engine.dust;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** CPU implementation of the bucketed dust frontier settle. */
public final class CpuDustPropagator implements DustPropagator {
  private int nodeCount;

  private int[] nodeIslandIds = new int[0];
  private int[][] nodesByIsland = new int[0][];
  private boolean[] islandDirty = new boolean[0];

  private int[] edgeIndex = new int[0];
  private int[] edgeTargets = new int[0];
  private int[] edgeWeights = new int[0];

  private int[] settledLevels = new int[0];
  private int[] previousLevels = new int[0];
  private int[] sourceLevels = new int[0];
  private int[] pendingSourceLevels = new int[0];
  private boolean[] touchedSourceFlags = new boolean[0];

  private final IntQueue[] buckets = new IntQueue[MAX_POWER_LEVEL + 1];
  private final IntArrayList changedNodes = new IntArrayList();
  private final IntArrayList dirtyIslands = new IntArrayList();
  private final IntArrayList touchedNodes = new IntArrayList();

  public CpuDustPropagator() {
    for (int level = 0; level < buckets.length; level++) {
      buckets[level] = new IntQueue();
    }
  }

  @Override
  public void reset(DustCsrGraph graph) {
    Objects.requireNonNull(graph, "graph");
    nodeCount = graph.nodeCount();

    settledLevels = new int[nodeCount];
    previousLevels = new int[nodeCount];
    sourceLevels = new int[nodeCount];
    pendingSourceLevels = new int[nodeCount];
    touchedSourceFlags = new boolean[nodeCount];

    nodeIslandIds = new int[nodeCount];
    for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
      nodeIslandIds[nodeId] = graph.islandId(nodeId);
    }
    nodesByIsland = buildNodesByIsland(nodeIslandIds);
    islandDirty = new boolean[nodesByIsland.length];

    edgeIndex = graph.edgeIndex();
    edgeTargets = graph.edgeTargets();
    edgeWeights = graph.edgeWeights();

    changedNodes.clear();
    dirtyIslands.clear();
    touchedNodes.clear();
  }

  @Override
  public int[] propagate(List<Source> changedSources) {
    Objects.requireNonNull(changedSources, "changedSources");
    if (changedSources.isEmpty()) {
      return new int[0];
    }

    touchedNodes.clear();
    for (int i = 0; i < changedSources.size(); i++) {
      Source source = DustPropagator.requireNonNull(changedSources.get(i));
      int nodeId = source.nodeId();
      if (nodeId >= nodeCount) {
        throw new IllegalArgumentException(
            "nodeId " + nodeId + " out of bounds (nodeCount=" + nodeCount + ")");
      }
      if (!touchedSourceFlags[nodeId]) {
        touchedSourceFlags[nodeId] = true;
        touchedNodes.add(nodeId);
      }
      pendingSourceLevels[nodeId] = source.powerLevel();
    }

    boolean anyDirty = false;
    for (int i = 0; i < touchedNodes.size(); i++) {
      int nodeId = touchedNodes.get(i);
      int newLevel = pendingSourceLevels[nodeId];
      int oldLevel = sourceLevels[nodeId];
      touchedSourceFlags[nodeId] = false;
      if (newLevel == oldLevel) {
        continue;
      }
      sourceLevels[nodeId] = newLevel;
      markIslandDirty(nodeIslandIds[nodeId]);
      anyDirty = true;
    }
    touchedNodes.clear();

    if (!anyDirty) {
      return new int[0];
    }

    changedNodes.clear();
    for (int i = 0; i < dirtyIslands.size(); i++) {
      int islandId = dirtyIslands.get(i);
      settleIsland(islandId);
      islandDirty[islandId] = false;
    }
    dirtyIslands.clear();

    return changedNodes.toArray();
  }

  @Override
  public int powerLevel(int nodeId) {
    if (nodeId < 0 || nodeId >= nodeCount) {
      throw new IllegalArgumentException("nodeId " + nodeId + " out of bounds");
    }
    return settledLevels[nodeId];
  }

  private void markIslandDirty(int islandId) {
    if (islandId < 0 || islandId >= islandDirty.length) {
      return;
    }
    if (!islandDirty[islandId]) {
      islandDirty[islandId] = true;
      dirtyIslands.add(islandId);
    }
  }

  private void settleIsland(int islandId) {
    int[] nodes = islandId < nodesByIsland.length ? nodesByIsland[islandId] : null;
    if (nodes == null || nodes.length == 0) {
      return;
    }

    for (IntQueue bucket : buckets) {
      bucket.clear();
    }

    for (int nodeId : nodes) {
      previousLevels[nodeId] = settledLevels[nodeId];
      settledLevels[nodeId] = 0;
    }

    for (int nodeId : nodes) {
      int level = sourceLevels[nodeId];
      if (level > 0) {
        buckets[level].add(nodeId);
      }
    }

    for (int level = MAX_POWER_LEVEL; level >= 0; level--) {
      IntQueue queue = buckets[level];
      while (!queue.isEmpty()) {
        int nodeId = queue.poll();
        if (level <= settledLevels[nodeId]) {
          continue;
        }
        settledLevels[nodeId] = level;
        int edgeStart = edgeIndex[nodeId];
        int edgeEnd = edgeIndex[nodeId + 1];
        for (int edge = edgeStart; edge < edgeEnd; edge++) {
          int dst = edgeTargets[edge];
          if (nodeIslandIds[dst] != islandId) {
            continue;
          }
          int newLevel = level - edgeWeights[edge];
          if (newLevel <= 0) {
            continue;
          }
          if (newLevel > settledLevels[dst]) {
            buckets[newLevel].add(dst);
          }
        }
      }
    }

    for (int nodeId : nodes) {
      if (settledLevels[nodeId] != previousLevels[nodeId]) {
        changedNodes.add(nodeId);
      }
    }
  }

  private static int[][] buildNodesByIsland(int[] nodeIslandIds) {
    if (nodeIslandIds.length == 0) {
      return new int[0][];
    }
    int maxIsland = 0;
    for (int islandId : nodeIslandIds) {
      if (islandId > maxIsland) {
        maxIsland = islandId;
      }
    }
    IntArrayList[] nodes = new IntArrayList[maxIsland + 1];
    for (int nodeId = 0; nodeId < nodeIslandIds.length; nodeId++) {
      int islandId = nodeIslandIds[nodeId];
      IntArrayList list = nodes[islandId];
      if (list == null) {
        list = new IntArrayList();
        nodes[islandId] = list;
      }
      list.add(nodeId);
    }
    int[][] result = new int[maxIsland + 1][];
    for (int islandId = 0; islandId <= maxIsland; islandId++) {
      IntArrayList list = nodes[islandId];
      result[islandId] = list != null ? list.toArray() : new int[0];
    }
    return result;
  }

  private static final class IntQueue {
    private static final int INITIAL_CAPACITY = 8;

    private int[] elements = new int[INITIAL_CAPACITY];
    private int head;
    private int tail;

    void add(int value) {
      ensureCapacity();
      elements[tail++] = value;
    }

    int poll() {
      if (isEmpty()) {
        throw new IllegalStateException("Queue is empty");
      }
      int value = elements[head++];
      if (head == tail) {
        head = 0;
        tail = 0;
      }
      return value;
    }

    boolean isEmpty() {
      return head == tail;
    }

    void clear() {
      head = 0;
      tail = 0;
    }

    private void ensureCapacity() {
      if (tail < elements.length) {
        return;
      }
      if (head > 0) {
        int size = tail - head;
        System.arraycopy(elements, head, elements, 0, size);
        head = 0;
        tail = size;
        return;
      }
      int newCapacity = elements.length * 2;
      elements = Arrays.copyOf(elements, newCapacity);
    }
  }

  private static final class IntArrayList {
    private static final int INITIAL_CAPACITY = 8;

    private int[] elements = new int[INITIAL_CAPACITY];
    private int size;

    void add(int value) {
      if (size == elements.length) {
        elements = Arrays.copyOf(elements, size * 2);
      }
      elements[size++] = value;
    }

    int get(int index) {
      if (index < 0 || index >= size) {
        throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
      }
      return elements[index];
    }

    int size() {
      return size;
    }

    void clear() {
      size = 0;
    }

    int[] toArray() {
      return Arrays.copyOf(elements, size);
    }
  }
}
