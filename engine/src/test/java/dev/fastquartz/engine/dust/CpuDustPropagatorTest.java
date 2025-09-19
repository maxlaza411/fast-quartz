package dev.fastquartz.engine.dust;

import static org.junit.jupiter.api.Assertions.*;

import dev.fastquartz.engine.world.BlockPos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CpuDustPropagatorTest {
  private CpuDustPropagator propagator;

  @BeforeEach
  void setUp() {
    propagator = new CpuDustPropagator();
  }

  @Test
  void singleSourceLinePropagatesAndIsIdempotent() {
    DustCsrGraph graph = lineGraph();
    propagator.reset(graph);

    int[] changed = propagator.propagate(List.of(DustPropagator.Source.of(0, 15)));
    assertArrayEquals(new int[] {0, 1, 2}, changed);
    assertEquals(15, propagator.powerLevel(0));
    assertEquals(14, propagator.powerLevel(1));
    assertEquals(13, propagator.powerLevel(2));

    int[] second = propagator.propagate(List.of(DustPropagator.Source.of(0, 15)));
    assertEquals(0, second.length);
  }

  @Test
  void loweringSourceClearsFrontier() {
    DustCsrGraph graph = lineGraph();
    propagator.reset(graph);

    propagator.propagate(List.of(DustPropagator.Source.of(0, 15)));

    int[] changed = propagator.propagate(List.of(DustPropagator.Source.of(0, 0)));
    assertArrayEquals(new int[] {0, 1, 2}, changed);
    assertEquals(0, propagator.powerLevel(0));
    assertEquals(0, propagator.powerLevel(1));
    assertEquals(0, propagator.powerLevel(2));
  }

  @Test
  void multipleSourcesCombineViaMaxRelaxation() {
    DustCsrGraph graph = lineGraph();
    propagator.reset(graph);

    propagator.propagate(List.of(DustPropagator.Source.of(0, 0))); // ensure zero baseline

    int[] changed =
        propagator.propagate(
            List.of(DustPropagator.Source.of(0, 12), DustPropagator.Source.of(2, 15)));
    assertArrayEquals(new int[] {0, 1, 2}, changed);
    assertEquals(13, propagator.powerLevel(0));
    assertEquals(14, propagator.powerLevel(1));
    assertEquals(15, propagator.powerLevel(2));
  }

  @Test
  void updatesAreIsolatedByIsland() {
    DustCsrGraph graph = twoIslandGraph();
    propagator.reset(graph);

    int[] first = propagator.propagate(List.of(DustPropagator.Source.of(0, 11)));
    assertArrayEquals(new int[] {0, 1}, first);
    assertEquals(11, propagator.powerLevel(0));
    assertEquals(10, propagator.powerLevel(1));
    assertEquals(0, propagator.powerLevel(2));
    assertEquals(0, propagator.powerLevel(3));

    int[] second = propagator.propagate(List.of(DustPropagator.Source.of(3, 9)));
    assertArrayEquals(new int[] {2, 3}, second);
    assertEquals(11, propagator.powerLevel(0));
    assertEquals(10, propagator.powerLevel(1));
    assertEquals(8, propagator.powerLevel(2));
    assertEquals(9, propagator.powerLevel(3));
  }

  @Test
  void invalidUpdatesAreRejected() {
    DustCsrGraph graph = lineGraph();
    propagator.reset(graph);

    assertThrows(
        IllegalArgumentException.class,
        () -> propagator.propagate(List.of(DustPropagator.Source.of(5, 12))));

    assertThrows(
        IllegalArgumentException.class,
        () -> propagator.propagate(List.of(DustPropagator.Source.of(0, 16))));

    assertThrows(
        IllegalArgumentException.class,
        () -> propagator.propagate(List.of(DustPropagator.Source.of(-1, 5))));
  }

  private static DustCsrGraph lineGraph() {
    List<BlockPos> positions =
        List.of(BlockPos.of(0, 0, 0), BlockPos.of(1, 0, 0), BlockPos.of(2, 0, 0));
    int[] islandIds = {0, 0, 0};
    int[] edgeIndex = {0, 1, 3, 4};
    int[] edgeTargets = {1, 0, 2, 1};
    int[] edgeWeights = {1, 1, 1, 1};
    Map<DustPort, Integer> portToNode = Map.of();
    Map<BlockPos, Integer> positionToNode =
        Map.of(positions.get(0), 0, positions.get(1), 1, positions.get(2), 2);
    return new DustCsrGraph(
        positions, islandIds, edgeIndex, edgeTargets, edgeWeights, portToNode, positionToNode);
  }

  private static DustCsrGraph twoIslandGraph() {
    List<BlockPos> positions =
        List.of(
            BlockPos.of(0, 0, 0),
            BlockPos.of(1, 0, 0),
            BlockPos.of(10, 0, 0),
            BlockPos.of(11, 0, 0));
    int[] islandIds = {0, 0, 1, 1};
    int[] edgeIndex = {0, 1, 2, 3, 4};
    int[] edgeTargets = {1, 0, 3, 2};
    int[] edgeWeights = {1, 1, 1, 1};
    Map<DustPort, Integer> portToNode = Map.of();
    Map<BlockPos, Integer> positionToNode =
        Map.of(
            positions.get(0), 0,
            positions.get(1), 1,
            positions.get(2), 2,
            positions.get(3), 3);
    return new DustCsrGraph(
        positions, islandIds, edgeIndex, edgeTargets, edgeWeights, portToNode, positionToNode);
  }
}
