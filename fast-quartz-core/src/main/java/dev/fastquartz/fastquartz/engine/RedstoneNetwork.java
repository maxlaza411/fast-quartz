package dev.fastquartz.fastquartz.engine;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Mutable graph structure representing the redstone network under simulation. */
public final class RedstoneNetwork {
  private final NavigableMap<BlockPos, Node> nodes = new TreeMap<>();

  /** Registers a new component at the provided position. */
  public void addComponent(BlockPos position, RedstoneComponent component) {
    Objects.requireNonNull(position, "position");
    Objects.requireNonNull(component, "component");
    if (nodes.containsKey(position)) {
      throw new IllegalArgumentException("Component already registered at " + position);
    }
    nodes.put(position, new Node(component));
  }

  /** Declares a directed connection from {@code source} to {@code target}. */
  public void connect(BlockPos source, BlockPos target) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
    Node sourceNode = requireNode(source);
    Node targetNode = requireNode(target);
    sourceNode.outputs.add(target);
    targetNode.inputs.add(source);
  }

  /** Returns the current power at the specified position. */
  public int powerAt(BlockPos position) {
    return requireNode(position).power;
  }

  /** Returns an immutable, sorted view of the registered positions. */
  public NavigableSet<BlockPos> positions() {
    return Collections.unmodifiableNavigableSet(nodes.navigableKeySet());
  }

  boolean hasComponent(BlockPos position) {
    return nodes.containsKey(position);
  }

  Node requireNode(BlockPos position) {
    Node node = nodes.get(position);
    if (node == null) {
      throw new IllegalArgumentException("No component registered at " + position);
    }
    return node;
  }

  Node nodeIfPresent(BlockPos position) {
    return nodes.get(position);
  }

  static final class Node {
    final RedstoneComponent component;
    final SortedSet<BlockPos> inputs = new TreeSet<>();
    final SortedSet<BlockPos> outputs = new TreeSet<>();
    int power;

    Node(RedstoneComponent component) {
      this.component = component;
    }
  }
}
