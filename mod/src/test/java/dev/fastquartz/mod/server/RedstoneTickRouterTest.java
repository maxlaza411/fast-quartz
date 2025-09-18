package dev.fastquartz.mod.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.fastquartz.engine.FastQuartzEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ticks.TickPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedstoneTickRouterTest {
  private ServerWorld world;
  private ServerTickScheduler scheduler;

  @BeforeEach
  void setUp() {
    world = new ServerWorld(FastQuartzEngine.create(20));
    scheduler = world.blockTickScheduler();
  }

  @Test
  void scheduledTickRunsAfterRequestedDelay() {
    List<Long> executedTicks = new ArrayList<>();
    ServerTickScheduler.ScheduledTickReceiver receiver =
        (serverWorld, pos) -> executedTicks.add(serverWorld.currentTick());

    scheduler.scheduleTick(BlockPos.of(0, 64, 0), receiver, 2, TickPriority.NORMAL);

    tickWorld(1);
    assertEquals(List.of(), executedTicks);

    tickWorld(1);
    assertEquals(List.of(2L), executedTicks);
  }

  @Test
  void schedulingDuringTickRunsInSameTickAfterCurrentTask() {
    List<String> events = new ArrayList<>();
    ServerTickScheduler.ScheduledTickReceiver secondary =
        (serverWorld, pos) -> events.add("secondary@" + serverWorld.currentTick());

    ServerTickScheduler.ScheduledTickReceiver primary =
        new ServerTickScheduler.ScheduledTickReceiver() {
          private boolean scheduled;

          @Override
          public void run(ServerWorld serverWorld, BlockPos pos) {
            events.add("primary@" + serverWorld.currentTick());
            if (!scheduled) {
              scheduled = true;
              scheduler.scheduleTick(pos, secondary, 0, TickPriority.NORMAL);
            }
          }
        };

    scheduler.scheduleTick(BlockPos.of(1, 70, 1), primary, 0, TickPriority.NORMAL);
    tickWorld(1);

    assertEquals(List.of("primary@1", "secondary@1"), events);
  }

  @Test
  void higherPriorityTicksExecuteFirstWithinSameTick() {
    List<String> events = new ArrayList<>();
    BlockPos pos = BlockPos.of(2, 64, 2);

    scheduler.scheduleTick(pos, (world, p) -> events.add("low"), 1, TickPriority.LOW);
    scheduler.scheduleTick(pos, (world, p) -> events.add("high"), 1, TickPriority.HIGH);

    tickWorld(1);

    assertEquals(List.of("high", "low"), events);
  }

  @Test
  void neighborPathDoesNotRecurseIntoVanillaCallStack() {
    List<String> events = new ArrayList<>();
    AtomicBoolean primaryRunning = new AtomicBoolean();

    ServerTickScheduler.ScheduledTickReceiver secondary =
        (serverWorld, pos) -> {
          events.add("secondary@" + serverWorld.currentTick());
          assertFalse(primaryRunning.get(), "secondary executed during primary recursion");
        };

    ServerTickScheduler.ScheduledTickReceiver primary =
        (serverWorld, pos) -> {
          primaryRunning.set(true);
          events.add("primary@" + serverWorld.currentTick());
          try {
            BlockPos neighbor = BlockPos.of(pos.getX() + 1, pos.getY(), pos.getZ());
            scheduler.scheduleTick(neighbor, secondary, 0, TickPriority.HIGH);
          } finally {
            primaryRunning.set(false);
          }
        };

    scheduler.scheduleTick(BlockPos.of(3, 64, 3), primary, 0, TickPriority.NORMAL);
    tickWorld(1);

    assertEquals(List.of("primary@1", "secondary@1"), events);
  }

  private void tickWorld(int ticks) {
    for (int i = 0; i < ticks; i++) {
      world.tick(() -> true);
    }
  }
}
