package dev.fastquartz.engine.dust;

import static org.junit.jupiter.api.Assertions.*;

import dev.fastquartz.engine.world.BlockPos;
import java.util.List;
import org.junit.jupiter.api.Test;

class DustCsrBuilderTest {
  @Test
  void compressesLongBusIntoTwoNodes() {
    int length = 64;
    DustCsrBuilder builder = new DustCsrBuilder();
    for (int x = 0; x < length; x++) {
      builder.addDust(BlockPos.of(x, 0, 0));
    }
    DustPort source = new DustPort(1, 0);
    DustPort sink = new DustPort(2, 0);
    builder.attachPort(source, BlockPos.of(0, 0, 0));
    builder.attachPort(sink, BlockPos.of(length - 1, 0, 0));

    DustCsrGraph graph = builder.build();

    assertEquals(2, graph.nodeCount());
    assertEquals(BlockPos.of(0, 0, 0), graph.nodePosition(0));
    assertEquals(BlockPos.of(length - 1, 0, 0), graph.nodePosition(1));
    assertEquals(0, graph.nodeForPort(source).orElseThrow());
    assertEquals(1, graph.nodeForPort(sink).orElseThrow());

    int[] edgeIndex = graph.edgeIndex();
    int[] edgeTargets = graph.edgeTargets();
    int[] edgeWeights = graph.edgeWeights();

    assertEquals(0, edgeIndex[0]);
    assertEquals(1, edgeIndex[1]);
    assertEquals(2, edgeIndex[2]);

    assertEquals(1, edgeTargets[0]);
    assertEquals(length - 1, edgeWeights[0]);
    assertEquals(0, edgeTargets[1]);
    assertEquals(length - 1, edgeWeights[1]);

    double reduction = (double) length / graph.nodeCount();
    assertTrue(reduction >= 5.0, "Compression ratio should be at least 5x");
  }

  @Test
  void attachmentsSplitStraightRun() {
    DustCsrBuilder builder = new DustCsrBuilder();
    for (int x = 0; x <= 4; x++) {
      builder.addDust(BlockPos.of(x, 0, 0));
    }
    DustPort start = new DustPort(10, 0);
    DustPort mid = new DustPort(11, 0);
    DustPort end = new DustPort(12, 0);
    builder.attachPort(start, BlockPos.of(0, 0, 0));
    builder.attachPort(mid, BlockPos.of(2, 0, 0));
    builder.attachPort(end, BlockPos.of(4, 0, 0));

    DustCsrGraph graph = builder.build();

    int startNode = graph.nodeForPort(start).orElseThrow();
    int midNode = graph.nodeForPort(mid).orElseThrow();
    int endNode = graph.nodeForPort(end).orElseThrow();

    assertEquals(3, graph.nodeCount());
    assertEquals(BlockPos.of(0, 0, 0), graph.nodePosition(startNode));
    assertEquals(BlockPos.of(2, 0, 0), graph.nodePosition(midNode));
    assertEquals(BlockPos.of(4, 0, 0), graph.nodePosition(endNode));

    List<DustCsrGraph.Edge> startEdges = graph.edgesFrom(startNode);
    assertEquals(1, startEdges.size());
    assertEquals(midNode, startEdges.get(0).targetNode());
    assertEquals(2, startEdges.get(0).weight());

    List<DustCsrGraph.Edge> midEdges = graph.edgesFrom(midNode);
    assertEquals(2, midEdges.size());
    assertTrue(midEdges.stream().anyMatch(e -> e.targetNode() == startNode && e.weight() == 2));
    assertTrue(midEdges.stream().anyMatch(e -> e.targetNode() == endNode && e.weight() == 2));
  }

  @Test
  void junctionProducesStarGraph() {
    DustCsrBuilder builder = new DustCsrBuilder();
    BlockPos center = BlockPos.of(0, 0, 0);
    BlockPos north = BlockPos.of(0, 0, -1);
    BlockPos south = BlockPos.of(0, 0, 1);
    BlockPos east = BlockPos.of(1, 0, 0);
    BlockPos west = BlockPos.of(-1, 0, 0);

    builder
        .addDust(center)
        .addDust(north)
        .addDust(south)
        .addDust(east)
        .addDust(west);

    DustPort northPort = new DustPort(20, 0);
    DustPort southPort = new DustPort(21, 0);
    DustPort eastPort = new DustPort(22, 0);
    DustPort westPort = new DustPort(23, 0);
    builder.attachPort(northPort, north);
    builder.attachPort(southPort, south);
    builder.attachPort(eastPort, east);
    builder.attachPort(westPort, west);

    DustCsrGraph graph = builder.build();

    assertEquals(5, graph.nodeCount());
    int centerNode = graph.nodeForPosition(center).orElseThrow();
    List<DustCsrGraph.Edge> centerEdges = graph.edgesFrom(centerNode);
    assertEquals(4, centerEdges.size());
    assertTrue(centerEdges.stream().allMatch(edge -> edge.weight() == 1));

    int northNode = graph.nodeForPort(northPort).orElseThrow();
    int southNode = graph.nodeForPort(southPort).orElseThrow();
    int eastNode = graph.nodeForPort(eastPort).orElseThrow();
    int westNode = graph.nodeForPort(westPort).orElseThrow();

    assertTrue(
        graph.edgesFrom(northNode).stream()
            .anyMatch(edge -> edge.targetNode() == centerNode && edge.weight() == 1));
    assertTrue(
        graph.edgesFrom(southNode).stream()
            .anyMatch(edge -> edge.targetNode() == centerNode && edge.weight() == 1));
    assertTrue(
        graph.edgesFrom(eastNode).stream()
            .anyMatch(edge -> edge.targetNode() == centerNode && edge.weight() == 1));
    assertTrue(
        graph.edgesFrom(westNode).stream()
            .anyMatch(edge -> edge.targetNode() == centerNode && edge.weight() == 1));
  }

  @Test
  void attachingPortToMissingDustFails() {
    DustCsrBuilder builder = new DustCsrBuilder();
    builder.addDust(BlockPos.of(0, 0, 0));
    builder.attachPort(new DustPort(30, 0), BlockPos.of(5, 0, 0));

    assertThrows(IllegalStateException.class, builder::build);
  }
}
