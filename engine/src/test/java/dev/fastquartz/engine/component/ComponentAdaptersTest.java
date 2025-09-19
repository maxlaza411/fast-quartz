package dev.fastquartz.engine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.engine.world.BlockPos;
import dev.fastquartz.engine.world.ShadowWorld;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;

class ComponentAdaptersTest {
  @Test
  void torchAdapterStagesWritesAndNotifications() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld shadowWorld = new ShadowWorld(world);
    ComponentContext context =
        new ComponentContext(
            shadowWorld, new ContainerSnapshotRegistry(), new ObserverPulseTracker(), 10L);
    BlockPos torchPos = BlockPos.of(0, 64, 0);
    BlockPos neighbour = BlockPos.of(1, 64, 0);

    ComponentAdapters.ComponentAdapter adapter =
        ComponentAdapters.standard(
            (access, pos, stateBits) -> {
              assertEquals(torchPos, pos);
              assertEquals(12, stateBits);
              assertEquals(0, access.getBlockStateBits(pos));
              access.setBlockStateBits(pos, 99);
              access.scheduleTick(pos, 3, 1);
              access.markNeighborChanged(neighbour);
            });

    adapter.apply(context, torchPos, 12);

    assertEquals(99, shadowWorld.getBlockStateBits(torchPos));
    assertTrue(world.writes.isEmpty(), "writes should be delayed until commit");

    shadowWorld.commit();

    assertEquals(List.of(new BlockWrite(torchPos, 99)), world.writes);
    assertEquals(List.of(new ScheduledTickCall(torchPos, 3, 1)), world.scheduledTicks);
    assertEquals(
        List.of(new NeighborCall(/* pos= */ neighbour, /* source= */ torchPos)),
        world.neighbourNotifications);
  }

  @Test
  void comparatorAdapterSnapshotsContainersOnce() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld shadowWorld = new ShadowWorld(world);
    ComponentContext context =
        new ComponentContext(
            shadowWorld, new ContainerSnapshotRegistry(), new ObserverPulseTracker(), 20L);
    BlockPos comparatorPos = BlockPos.of(5, 70, 5);
    BlockPos containerPos = BlockPos.of(5, 70, 6);
    AtomicInteger counter = new AtomicInteger();
    world.setContainerSignal(containerPos, counter::incrementAndGet);

    List<Integer> observed = new ArrayList<>();
    ComponentAdapters.ComponentAdapter adapter =
        ComponentAdapters.comparator(
            (access, pos, stateBits) -> {
              observed.add(access.readContainerSignal(containerPos));
              access.setBlockStateBits(pos, 7);
              observed.add(access.readContainerSignal(containerPos));
            },
            (pos, stateBits) -> List.of(containerPos));

    adapter.apply(context, comparatorPos, 0);

    assertEquals(List.of(1, 1), observed);
    assertEquals(1, world.containerReads(containerPos));
    assertEquals(7, shadowWorld.getBlockStateBits(comparatorPos));
  }

  @Test
  void observerAdapterCoalescesPerTick() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld shadowWorld = new ShadowWorld(world);
    ObserverPulseTracker tracker = new ObserverPulseTracker();
    BlockPos observer = BlockPos.of(0, 70, 0);
    BlockPos observedA = BlockPos.of(1, 70, 0);
    BlockPos observedB = BlockPos.of(2, 70, 0);
    AtomicInteger pulses = new AtomicInteger();
    ComponentAdapters.ObserverAdapter adapter =
        ComponentAdapters.observer(
            (access, observerPos, observedPos, oldBits, newBits) -> {
              pulses.incrementAndGet();
              access.markNeighborChanged(observedPos);
            });

    ComponentContext tick10 =
        new ComponentContext(shadowWorld, new ContainerSnapshotRegistry(), tracker, 10L);
    adapter.apply(tick10, observer, observedA, 0, 1);
    adapter.apply(tick10, observer, observedA, 1, 2); // coalesced
    adapter.apply(tick10, observer, observedB, 3, 4); // different source → allowed
    adapter.apply(tick10, observer, observedB, 4, 4); // no change
    assertEquals(2, pulses.get());

    ComponentContext tick11 =
        new ComponentContext(shadowWorld, new ContainerSnapshotRegistry(), tracker, 11L);
    adapter.apply(tick11, observer, observedA, 5, 6); // new tick resets coalescing
    assertEquals(3, pulses.get());

    shadowWorld.commit();
    assertEquals(
        List.of(
            new NeighborCall(/* pos= */ observedA, /* source= */ observer),
            new NeighborCall(/* pos= */ observedB, /* source= */ observer),
            new NeighborCall(/* pos= */ observedA, /* source= */ observer)),
        world.neighbourNotifications);
  }

  @Test
  void pistonAdapterAppliesTransactionsDeterministically() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld shadowWorld = new ShadowWorld(world);
    ComponentContext context =
        new ComponentContext(
            shadowWorld, new ContainerSnapshotRegistry(), new ObserverPulseTracker(), 30L);
    BlockPos pistonPos = BlockPos.of(0, 64, 0);
    BlockPos source = BlockPos.of(1, 64, 0);
    BlockPos dest = BlockPos.of(2, 64, 0);
    world.prime(source, 55);

    ComponentAdapters.PistonAdapter adapter =
        ComponentAdapters.piston(
            (access, pos, stateBits) ->
                new ComponentAdapters.PistonTransaction(
                    List.of(new ComponentAdapters.PistonMove(source, dest, 77)),
                    List.of(BlockPos.of(3, 64, 0))));

    adapter.apply(context, pistonPos, 0);

    assertEquals(0, shadowWorld.getBlockStateBits(source));
    assertEquals(77, shadowWorld.getBlockStateBits(dest));

    shadowWorld.commit();

    assertEquals(List.of(new BlockWrite(source, 0), new BlockWrite(dest, 77)), world.writes);
    assertEquals(
        List.of(
            new NeighborCall(/* pos= */ source, /* source= */ pistonPos),
            new NeighborCall(/* pos= */ dest, /* source= */ pistonPos),
            new NeighborCall(/* pos= */ BlockPos.of(3, 64, 0), /* source= */ pistonPos)),
        world.neighbourNotifications);
  }

  @Test
  void pistonAdapterRejectsDuplicateSourcesOrDestinations() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld shadowWorld = new ShadowWorld(world);
    ComponentContext context =
        new ComponentContext(
            shadowWorld, new ContainerSnapshotRegistry(), new ObserverPulseTracker(), 40L);
    BlockPos pistonPos = BlockPos.of(0, 64, 0);
    BlockPos a = BlockPos.of(1, 64, 0);
    BlockPos b = BlockPos.of(2, 64, 0);
    BlockPos c = BlockPos.of(3, 64, 0);

    ComponentAdapters.PistonAdapter duplicateSources =
        ComponentAdapters.piston(
            (access, pos, stateBits) ->
                new ComponentAdapters.PistonTransaction(
                    List.of(
                        new ComponentAdapters.PistonMove(a, b, 1),
                        new ComponentAdapters.PistonMove(a, c, 2)),
                    List.of()));

    assertThrows(
        IllegalArgumentException.class, () -> duplicateSources.apply(context, pistonPos, 0));

    ComponentAdapters.PistonAdapter duplicateDestinations =
        ComponentAdapters.piston(
            (access, pos, stateBits) ->
                new ComponentAdapters.PistonTransaction(
                    List.of(
                        new ComponentAdapters.PistonMove(a, b, 1),
                        new ComponentAdapters.PistonMove(c, b, 2)),
                    List.of()));

    assertThrows(
        IllegalArgumentException.class, () -> duplicateDestinations.apply(context, pistonPos, 0));
  }

  private static final class RecordingWorld implements ShadowWorld.Delegate {
    private final Map<BlockPos, Integer> states = new HashMap<>();
    private final Map<BlockPos, IntSupplier> containerSignals = new HashMap<>();
    private final Map<BlockPos, Integer> containerReads = new HashMap<>();
    private final List<BlockWrite> writes = new ArrayList<>();
    private final List<NeighborCall> neighbourNotifications = new ArrayList<>();
    private final List<ScheduledTickCall> scheduledTicks = new ArrayList<>();

    @Override
    public int getBlockStateBits(BlockPos pos) {
      return states.getOrDefault(pos, 0);
    }

    @Override
    public void setBlockStateBits(BlockPos pos, int stateBits) {
      states.put(pos, stateBits);
      writes.add(new BlockWrite(pos, stateBits));
    }

    @Override
    public void scheduleTick(BlockPos pos, int delayTicks, int priority) {
      scheduledTicks.add(new ScheduledTickCall(pos, delayTicks, priority));
    }

    @Override
    public void markNeighborChanged(BlockPos pos, BlockPos source) {
      neighbourNotifications.add(new NeighborCall(pos, source));
    }

    @Override
    public int readContainerSignal(BlockPos pos) {
      IntSupplier supplier = containerSignals.get(pos);
      int value = supplier == null ? 0 : supplier.getAsInt();
      containerReads.merge(pos, 1, Integer::sum);
      return value;
    }

    void prime(BlockPos pos, int stateBits) {
      states.put(pos, stateBits);
    }

    void setContainerSignal(BlockPos pos, IntSupplier supplier) {
      containerSignals.put(pos, Objects.requireNonNull(supplier, "supplier"));
    }

    int containerReads(BlockPos pos) {
      return containerReads.getOrDefault(pos, 0);
    }
  }

  private record BlockWrite(BlockPos pos, int stateBits) {
    // Recording helper for block writes.
  }

  private record NeighborCall(BlockPos pos, BlockPos source) {
    // Recording helper for neighbour notifications.
  }

  private record ScheduledTickCall(BlockPos pos, int delayTicks, int priority) {
    // Recording helper for scheduled ticks.
  }
}
