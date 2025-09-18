package dev.fastquartz.engine.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShadowWorldTest {
  @Test
  void passthroughWhenUntouched() {
    RecordingWorld world = new RecordingWorld();
    BlockPos pos = BlockPos.of(1, 2, 3);
    world.prime(pos, 42);
    ShadowWorld overlay = new ShadowWorld(world);

    assertEquals(42, overlay.getBlockStateBits(pos));
  }

  @Test
  void setBlockStateBuffersUntilCommit() {
    RecordingWorld world = new RecordingWorld();
    BlockPos pos = BlockPos.of(4, 5, 6);
    world.prime(pos, 7);
    ShadowWorld overlay = new ShadowWorld(world);

    overlay.setBlockStateBits(pos, 9);
    assertEquals(9, overlay.getBlockStateBits(pos));
    assertTrue(world.writes.isEmpty());

    overlay.commit();

    assertEquals(9, world.getBlockStateBits(pos));
    assertEquals(List.of(new BlockWrite(pos, 9)), world.writes);
    assertEquals(9, overlay.getBlockStateBits(pos));
  }

  @Test
  void settingStateBackToBaseRemovesBufferedChange() {
    RecordingWorld world = new RecordingWorld();
    BlockPos pos = BlockPos.of(7, 8, 9);
    world.prime(pos, 15);
    ShadowWorld overlay = new ShadowWorld(world);

    overlay.setBlockStateBits(pos, 1);
    assertEquals(1, overlay.getBlockStateBits(pos));

    overlay.setBlockStateBits(pos, 15); // back to base value
    assertEquals(15, overlay.getBlockStateBits(pos));

    overlay.commit();
    assertTrue(world.writes.isEmpty());
  }

  @Test
  void commitAppliesWritesInDeterministicOrder() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld overlay = new ShadowWorld(world);

    BlockPos a = BlockPos.of(-1, -1, 0);
    BlockPos b = BlockPos.of(0, 0, 0);
    BlockPos c = BlockPos.of(1, 0, 0);
    BlockPos d = BlockPos.of(0, 0, 1);
    BlockPos e = BlockPos.of(5, 17, 5);
    BlockPos f = BlockPos.of(5, 32, 5);

    overlay.setBlockStateBits(b, 2);
    overlay.setBlockStateBits(f, 3);
    overlay.setBlockStateBits(d, 4);
    overlay.setBlockStateBits(a, 5);
    overlay.setBlockStateBits(e, 6);
    overlay.setBlockStateBits(c, 7);

    overlay.commit();

    assertEquals(
        List.of(
            new BlockWrite(a, 5),
            new BlockWrite(b, 2),
            new BlockWrite(c, 7),
            new BlockWrite(d, 4),
            new BlockWrite(e, 6),
            new BlockWrite(f, 3)),
        world.writes);
  }

  @Test
  void neighbourNotificationsFlushOnCommit() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld overlay = new ShadowWorld(world);
    BlockPos pos = BlockPos.of(2, 2, 2);
    BlockPos source = BlockPos.of(3, 3, 3);

    overlay.markNeighborChanged(pos, source);
    assertTrue(world.neighbourNotifications.isEmpty());

    overlay.commit();
    assertEquals(List.of(new NeighborCall(pos, source)), world.neighbourNotifications);

    int notifications = world.neighbourNotifications.size();
    overlay.commit();
    assertEquals(notifications, world.neighbourNotifications.size());
  }

  @Test
  void scheduledTicksFlushOnCommit() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld overlay = new ShadowWorld(world);
    BlockPos pos = BlockPos.of(9, 9, 9);

    overlay.scheduleTick(pos, 4, 1);
    assertTrue(world.scheduledTicks.isEmpty());

    overlay.commit();
    assertEquals(List.of(new ScheduledTickCall(pos, 4, 1)), world.scheduledTicks);

    int scheduled = world.scheduledTicks.size();
    overlay.commit();
    assertEquals(scheduled, world.scheduledTicks.size());
  }

  @Test
  void secondCommitWithNoChangesDoesNothing() {
    RecordingWorld world = new RecordingWorld();
    ShadowWorld overlay = new ShadowWorld(world);
    BlockPos pos = BlockPos.of(1, 1, 1);

    overlay.setBlockStateBits(pos, 12);
    overlay.commit();
    int writesAfterFirst = world.writes.size();
    overlay.commit();
    assertEquals(writesAfterFirst, world.writes.size());
  }

  private static final class RecordingWorld implements ShadowWorld.Delegate {
    private final Map<BlockPos, Integer> states = new HashMap<>();
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

    void prime(BlockPos pos, int stateBits) {
      states.put(pos, stateBits);
    }
  }

  private record BlockWrite(BlockPos pos, int stateBits) {}

  private record NeighborCall(BlockPos pos, BlockPos source) {}

  private record ScheduledTickCall(BlockPos pos, int delayTicks, int priority) {}
}
