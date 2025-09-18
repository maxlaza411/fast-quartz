package net.minecraft.server.world;

import dev.fastquartz.engine.FastQuartzEngine;
import dev.fastquartz.mod.server.RedstoneTickRouter;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Minimal stub of Minecraft's {@code ServerWorld} focused on scheduled tick redirection. */
public final class ServerWorld {
  private final FastQuartzEngine engine;
  private final RedstoneTickRouter redstoneRouter;
  private final ServerTickScheduler blockTickScheduler;
  private long currentTick;

  public ServerWorld(FastQuartzEngine engine) {
    this.engine = Objects.requireNonNull(engine, "engine");
    this.redstoneRouter = new RedstoneTickRouter(engine);
    this.blockTickScheduler = new ServerTickScheduler(this, redstoneRouter);
  }

  /** Runs one world tick and drains scheduled redstone work from the router. */
  public void tick(BooleanSupplier shouldKeepTicking) {
    Objects.requireNonNull(shouldKeepTicking, "shouldKeepTicking");
    currentTick++;
    redstoneRouter.runDueTicks(this);
  }

  public long currentTick() {
    return currentTick;
  }

  public FastQuartzEngine engine() {
    return engine;
  }

  public ServerTickScheduler blockTickScheduler() {
    return blockTickScheduler;
  }

  public RedstoneTickRouter redstoneRouter() {
    return redstoneRouter;
  }
}
