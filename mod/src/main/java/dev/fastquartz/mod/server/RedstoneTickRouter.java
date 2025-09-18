package dev.fastquartz.mod.server;

import dev.fastquartz.engine.FastQuartzEngine;
import dev.fastquartz.engine.event.EventKey;
import dev.fastquartz.engine.event.EventQueue;
import dev.fastquartz.engine.event.EventType;
import java.util.Objects;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ticks.TickPriority;

/** Routes scheduled ticks from the stubbed server world into the Fast Quartz event queue. */
public final class RedstoneTickRouter {
  private static final int REGION_BLOCK_SIZE = 64; // 4×4 chunks.

  private final FastQuartzEngine engine;
  private final EventQueue<ScheduledTick> queue = new EventQueue<>();
  private long activeTick = Long.MIN_VALUE;
  private boolean draining;

  public RedstoneTickRouter(FastQuartzEngine engine) {
    this.engine = Objects.requireNonNull(engine, "engine");
  }

  public FastQuartzEngine engine() {
    return engine;
  }

  public void clear() {
    queue.clear();
    activeTick = Long.MIN_VALUE;
    draining = false;
  }

  public void schedule(
      ServerWorld world,
      BlockPos pos,
      int delayTicks,
      TickPriority priority,
      ServerTickScheduler.ScheduledTickReceiver receiver) {
    Objects.requireNonNull(world, "world");
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(priority, "priority");
    Objects.requireNonNull(receiver, "receiver");

    long baseline = world.currentTick();
    if (draining && activeTick > baseline) {
      baseline = activeTick;
    }

    long dueTick = baseline + Math.max(delayTicks, 0);
    EventKey key =
        EventKey.forBlock(
            dueTick,
            microPhase(priority),
            regionId(pos),
            localX(pos),
            pos.getY(),
            localZ(pos),
            EventType.SCHEDULED);
    queue.schedule(key, new ScheduledTick(world, pos, receiver));
  }

  public void runDueTicks(ServerWorld world) {
    Objects.requireNonNull(world, "world");
    activeTick = world.currentTick();
    EventQueue.Event<ScheduledTick> next;
    while ((next = queue.peek()) != null && next.key().tick() <= activeTick) {
      queue.poll();
      draining = true;
      try {
        next.payload().run();
      } finally {
        draining = false;
      }
    }
  }

  private static int microPhase(TickPriority priority) {
    return Math.max(0, Math.min(9, priority.value()));
  }

  private static int regionId(BlockPos pos) {
    int regionX = Math.floorDiv(pos.getX(), REGION_BLOCK_SIZE);
    int regionZ = Math.floorDiv(pos.getZ(), REGION_BLOCK_SIZE);
    return ((regionX & 0xFFFF) << 16) | (regionZ & 0xFFFF);
  }

  private static int localX(BlockPos pos) {
    return Math.floorMod(pos.getX(), REGION_BLOCK_SIZE);
  }

  private static int localZ(BlockPos pos) {
    return Math.floorMod(pos.getZ(), REGION_BLOCK_SIZE);
  }

  private record ScheduledTick(
      ServerWorld world, BlockPos pos, ServerTickScheduler.ScheduledTickReceiver receiver) {
    void run() {
      receiver.run(world, pos);
    }
  }
}
