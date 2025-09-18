package net.minecraft.server.world;

import dev.fastquartz.mod.server.RedstoneTickRouter;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ticks.TickPriority;

/** Minimal hook surface for {@code ServerTickScheduler}. */
public final class ServerTickScheduler {
  private final ServerWorld world;
  private final RedstoneTickRouter router;

  ServerTickScheduler(ServerWorld world, RedstoneTickRouter router) {
    this.world = Objects.requireNonNull(world, "world");
    this.router = Objects.requireNonNull(router, "router");
  }

  public void scheduleTick(
      BlockPos pos, ScheduledTickReceiver receiver, int delay, TickPriority priority) {
    router.schedule(world, pos, delay, priority, receiver);
  }

  public void scheduleTick(BlockPos pos, ScheduledTickReceiver receiver, int delay) {
    scheduleTick(pos, receiver, delay, TickPriority.NORMAL);
  }

  public interface ScheduledTickReceiver {
    void run(ServerWorld world, BlockPos pos);
  }
}
