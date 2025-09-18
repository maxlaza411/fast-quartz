package dev.fastquartz.engine.dust;

import dev.fastquartz.engine.world.BlockPos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds the compressed dust CSR graph from a world snapshot. */
public final class DustCsrBuilder {
  private static final int[][] NEIGHBOR_OFFSETS = {
    {0, 1, 0}, // up
    {0, -1, 0}, // down
    {0, 0, 1}, // south (positive Z)
    {0, 0, -1}, // north (negative Z)
    {1, 0, 0}, // east (positive X)
    {-1, 0, 0}, // west (negative X)
  };

  private static final Comparator<BlockPos> POSITION_ORDER =
      Comparator.comparingInt(BlockPos::y)
          .thenComparingInt(BlockPos::z)
          .thenComparingInt(BlockPos::x);

  private final Set<BlockPos> dustPositions = new HashSet<>();
  private final Map<BlockPos, List<DustPort>> attachmentsByPosition = new HashMap<>();
  private final Map<DustPort, BlockPos> portBindings = new HashMap<>();

  /** Adds a dust block to the builder. */
  public DustCsrBuilder addDust(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    dustPositions.add(pos);
    return this;
  }

  /** Adds all supplied dust blocks to the builder. */
  public DustCsrBuilder addAllDust(Collection<BlockPos> positions) {
    Objects.requireNonNull(positions, "positions");
    for (BlockPos pos : positions) {
      addDust(pos);
    }
    return this;
  }

  /** Attaches a component port to the provided dust block. */
  public DustCsrBuilder attachPort(DustPort port, BlockPos dustPos) {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(dustPos, "dustPos");
    BlockPos existing = portBindings.putIfAbsent(port, dustPos);
    if (existing != null && !existing.equals(dustPos)) {
      throw new IllegalArgumentException("Port " + port + " already attached to " + existing);
    }
    List<DustPort> ports = attachmentsByPosition.computeIfAbsent(dustPos, key -> new ArrayList<>());
    if (!ports.contains(port)) {
      ports.add(port);
    }
    return this;
  }

  /** Builds the compressed dust graph. */
  public DustCsrGraph build() {
    if (dustPositions.isEmpty()) {
      return DustCsrGraph.empty();
    }

    validateAttachmentPositions();

    Map<BlockPos, List<BlockPos>> adjacency = buildAdjacency();
    List<List<BlockPos>> islands = discoverIslands(adjacency);

    List<BlockPos> nodePositions = new ArrayList<>();
    List<Integer> nodeIslandIds = new ArrayList<>();
    Map<BlockPos, Integer> positionToNode = new HashMap<>();

    for (int islandId = 0; islandId < islands.size(); islandId++) {
      List<BlockPos> islandPositions = islands.get(islandId);
      Set<BlockPos> nodes = identifyNodes(islandPositions, adjacency);
      List<BlockPos> sortedNodes = new ArrayList<>(nodes);
      sortedNodes.sort(POSITION_ORDER);
      for (BlockPos pos : sortedNodes) {
        int nodeId = nodePositions.size();
        nodePositions.add(pos);
        nodeIslandIds.add(islandId);
        positionToNode.put(pos, nodeId);
      }
    }

    int nodeCount = nodePositions.size();
    if (nodeCount == 0) {
      // Degenerate case: dust exists but all nodes collapsed. Create a single node to represent it.
      BlockPos fallback = dustPositions.stream().min(POSITION_ORDER).orElseThrow();
      nodePositions.add(fallback);
      nodeIslandIds.add(0);
      positionToNode.put(fallback, 0);
      nodeCount = 1;
    }

    int[] edgeIndex = new int[nodeCount + 1];
    List<Integer> edgeTargetsList = new ArrayList<>();
    List<Integer> edgeWeightsList = new ArrayList<>();

    int maxSteps = dustPositions.size();

    for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
      BlockPos nodePos = nodePositions.get(nodeId);
      List<BlockPos> neighbours = adjacency.getOrDefault(nodePos, List.of());
      edgeIndex[nodeId] = edgeTargetsList.size();
      List<EdgeCandidate> candidates = new ArrayList<>();
      for (BlockPos neighbour : neighbours) {
        Traversal traversal = walk(nodePos, neighbour, adjacency, positionToNode, maxSteps);
        if (traversal == null) {
          continue;
        }
        Integer targetNode = positionToNode.get(traversal.target());
        if (targetNode == null || targetNode == nodeId) {
          continue;
        }
        candidates.add(new EdgeCandidate(targetNode, traversal.weight()));
      }
      candidates.sort(EdgeCandidate.ORDER);
      for (EdgeCandidate candidate : candidates) {
        edgeTargetsList.add(candidate.targetNode());
        edgeWeightsList.add(candidate.weight());
      }
    }
    edgeIndex[nodeCount] = edgeTargetsList.size();

    int[] islandIds = nodeIslandIds.stream().mapToInt(Integer::intValue).toArray();
    int[] edgeTargets = edgeTargetsList.stream().mapToInt(Integer::intValue).toArray();
    int[] edgeWeights = edgeWeightsList.stream().mapToInt(Integer::intValue).toArray();

    Map<DustPort, Integer> portToNode = buildPortMapping(positionToNode);

    return new DustCsrGraph(nodePositions, islandIds, edgeIndex, edgeTargets, edgeWeights, portToNode, positionToNode);
  }

  private void validateAttachmentPositions() {
    for (Map.Entry<DustPort, BlockPos> entry : portBindings.entrySet()) {
      if (!dustPositions.contains(entry.getValue())) {
        throw new IllegalStateException(
            "Port " + entry.getKey() + " attached to non-dust position " + entry.getValue());
      }
    }
  }

  private Map<BlockPos, List<BlockPos>> buildAdjacency() {
    Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
    for (BlockPos pos : dustPositions) {
      List<BlockPos> neighbours = new ArrayList<>();
      for (int[] offset : NEIGHBOR_OFFSETS) {
        BlockPos neighbour =
            BlockPos.of(pos.x() + offset[0], pos.y() + offset[1], pos.z() + offset[2]);
        if (dustPositions.contains(neighbour)) {
          neighbours.add(neighbour);
        }
      }
      neighbours.sort(POSITION_ORDER);
      adjacency.put(pos, neighbours);
    }
    return adjacency;
  }

  private List<List<BlockPos>> discoverIslands(Map<BlockPos, List<BlockPos>> adjacency) {
    List<BlockPos> sorted = new ArrayList<>(dustPositions);
    sorted.sort(POSITION_ORDER);
    Set<BlockPos> visited = new HashSet<>();
    List<List<BlockPos>> islands = new ArrayList<>();

    for (BlockPos start : sorted) {
      if (!visited.add(start)) {
        continue;
      }
      List<BlockPos> island = new ArrayList<>();
      Deque<BlockPos> queue = new ArrayDeque<>();
      queue.add(start);
      while (!queue.isEmpty()) {
        BlockPos current = queue.removeFirst();
        island.add(current);
        for (BlockPos neighbour : adjacency.getOrDefault(current, List.of())) {
          if (visited.add(neighbour)) {
            queue.addLast(neighbour);
          }
        }
      }
      island.sort(POSITION_ORDER);
      islands.add(island);
    }

    return islands;
  }

  private Set<BlockPos> identifyNodes(
      List<BlockPos> islandPositions, Map<BlockPos, List<BlockPos>> adjacency) {
    Set<BlockPos> nodes = new HashSet<>();
    for (BlockPos pos : islandPositions) {
      List<BlockPos> neighbours = adjacency.getOrDefault(pos, List.of());
      int degree = neighbours.size();
      boolean hasAttachment = attachmentsByPosition.containsKey(pos);
      boolean isCorner = false;
      if (degree == 2) {
        BlockPos first = neighbours.get(0);
        BlockPos second = neighbours.get(1);
        int dx1 = first.x() - pos.x();
        int dy1 = first.y() - pos.y();
        int dz1 = first.z() - pos.z();
        int dx2 = second.x() - pos.x();
        int dy2 = second.y() - pos.y();
        int dz2 = second.z() - pos.z();
        boolean opposite = dx1 == -dx2 && dy1 == -dy2 && dz1 == -dz2;
        isCorner = !opposite;
      }
      if (degree != 2 || hasAttachment || isCorner) {
        nodes.add(pos);
      }
    }

    if (nodes.isEmpty() && !islandPositions.isEmpty()) {
      BlockPos first = islandPositions.get(0);
      nodes.add(first);
      List<BlockPos> neighbours = adjacency.getOrDefault(first, List.of());
      if (!neighbours.isEmpty()) {
        nodes.add(neighbours.get(0));
      }
    }

    return nodes;
  }

  private Traversal walk(
      BlockPos start,
      BlockPos next,
      Map<BlockPos, List<BlockPos>> adjacency,
      Map<BlockPos, Integer> positionToNode,
      int maxSteps) {
    BlockPos previous = start;
    BlockPos current = next;
    int weight = 1;
    int steps = 0;
    while (!positionToNode.containsKey(current)) {
      List<BlockPos> neighbours = adjacency.get(current);
      if (neighbours == null || neighbours.isEmpty()) {
        return null;
      }
      BlockPos candidate = null;
      for (BlockPos neighbour : neighbours) {
        if (!neighbour.equals(previous)) {
          candidate = neighbour;
          break;
        }
      }
      if (candidate == null) {
        return null;
      }
      previous = current;
      current = candidate;
      weight++;
      if (++steps > maxSteps) {
        throw new IllegalStateException("Traversal exceeded dust graph bounds starting from " + start);
      }
    }
    return new Traversal(current, weight);
  }

  private Map<DustPort, Integer> buildPortMapping(Map<BlockPos, Integer> positionToNode) {
    Map<DustPort, Integer> mapping = new LinkedHashMap<>();
    List<Map.Entry<BlockPos, List<DustPort>>> entries = new ArrayList<>(attachmentsByPosition.entrySet());
    entries.sort((a, b) -> POSITION_ORDER.compare(a.getKey(), b.getKey()));
    for (Map.Entry<BlockPos, List<DustPort>> entry : entries) {
      BlockPos pos = entry.getKey();
      Integer nodeId = positionToNode.get(pos);
      if (nodeId == null) {
        throw new IllegalStateException("No node for attachment at " + pos);
      }
      List<DustPort> ports = new ArrayList<>(entry.getValue());
      ports.sort(DustPort::compare);
      for (DustPort port : ports) {
        mapping.put(port, nodeId);
      }
    }
    return mapping;
  }

  private record Traversal(BlockPos target, int weight) {}

  private record EdgeCandidate(int targetNode, int weight) {
    private static final Comparator<EdgeCandidate> ORDER =
        Comparator.comparingInt(EdgeCandidate::targetNode).thenComparingInt(EdgeCandidate::weight);
  }
}
