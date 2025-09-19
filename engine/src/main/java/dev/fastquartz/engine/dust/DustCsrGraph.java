package dev.fastquartz.engine.dust;

import dev.fastquartz.engine.world.BlockPos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

/** Immutable compressed sparse row representation of the dust network. */
public final class DustCsrGraph {
  private static final DustCsrGraph EMPTY =
      new DustCsrGraph(
          List.of(),
          new int[0],
          new int[] {0},
          new int[0],
          new int[0],
          Collections.emptyMap(),
          Collections.emptyMap());

  private final List<BlockPos> nodePositions;
  private final int[] islandIds;
  private final int[] edgeIndex;
  private final int[] edgeTargets;
  private final int[] edgeWeights;
  private final Map<DustPort, Integer> portToNode;
  private final Map<BlockPos, Integer> positionToNode;

  DustCsrGraph(
      List<BlockPos> nodePositions,
      int[] islandIds,
      int[] edgeIndex,
      int[] edgeTargets,
      int[] edgeWeights,
      Map<DustPort, Integer> portToNode,
      Map<BlockPos, Integer> positionToNode) {
    this.nodePositions = List.copyOf(nodePositions);
    this.islandIds = islandIds.clone();
    this.edgeIndex = edgeIndex.clone();
    this.edgeTargets = edgeTargets.clone();
    this.edgeWeights = edgeWeights.clone();
    this.portToNode = Map.copyOf(portToNode);
    this.positionToNode = Map.copyOf(positionToNode);

    int nodeCount = this.nodePositions.size();
    if (this.islandIds.length != nodeCount) {
      throw new IllegalArgumentException("islandIds must align with node positions");
    }
    if (this.edgeIndex.length != nodeCount + 1) {
      throw new IllegalArgumentException("edgeIndex must be nodeCount + 1");
    }
    if (this.edgeTargets.length != this.edgeWeights.length) {
      throw new IllegalArgumentException("edgeTargets and edgeWeights length mismatch");
    }
  }

  /** Returns an empty graph with no nodes or edges. */
  public static DustCsrGraph empty() {
    return EMPTY;
  }

  public int nodeCount() {
    return nodePositions.size();
  }

  public int edgeCount() {
    return edgeTargets.length;
  }

  public BlockPos nodePosition(int nodeId) {
    return nodePositions.get(nodeId);
  }

  public int islandId(int nodeId) {
    return islandIds[nodeId];
  }

  public int[] edgeIndex() {
    return edgeIndex.clone();
  }

  public int[] edgeTargets() {
    return edgeTargets.clone();
  }

  public int[] edgeWeights() {
    return edgeWeights.clone();
  }

  public List<Edge> edgesFrom(int nodeId) {
    int start = edgeIndex[nodeId];
    int end = edgeIndex[nodeId + 1];
    List<Edge> edges = new ArrayList<>(end - start);
    for (int i = start; i < end; i++) {
      edges.add(new Edge(edgeTargets[i], edgeWeights[i]));
    }
    return edges;
  }

  public OptionalInt nodeForPort(DustPort port) {
    Objects.requireNonNull(port, "port");
    Integer node = portToNode.get(port);
    return node != null ? OptionalInt.of(node) : OptionalInt.empty();
  }

  public OptionalInt nodeForPosition(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    Integer node = positionToNode.get(pos);
    return node != null ? OptionalInt.of(node) : OptionalInt.empty();
  }

  public Map<DustPort, Integer> portToNode() {
    return portToNode;
  }

  public Map<BlockPos, Integer> positionToNode() {
    return positionToNode;
  }

  /** Lightweight view of an outgoing edge. */
  public record Edge(int targetNode, int weight) {
    // Immutable edge descriptor.
  }
}
