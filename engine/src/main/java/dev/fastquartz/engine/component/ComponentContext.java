package dev.fastquartz.engine.component;

import dev.fastquartz.engine.world.BlockPos;
import dev.fastquartz.engine.world.ShadowWorld;
import java.util.Objects;

/**
 * Execution context shared by component adapters. It bundles the shadow world view and the
 * deterministic registries used during a micro-phase.
 */
public final class ComponentContext {
  private final ShadowWorld shadowWorld;
  private final ContainerSnapshotRegistry containerSnapshots;
  private final ObserverPulseTracker observerPulseTracker;
  private final long tick;

  public ComponentContext(
      ShadowWorld shadowWorld,
      ContainerSnapshotRegistry containerSnapshots,
      ObserverPulseTracker observerPulseTracker,
      long tick) {
    this.shadowWorld = Objects.requireNonNull(shadowWorld, "shadowWorld");
    this.containerSnapshots = Objects.requireNonNull(containerSnapshots, "containerSnapshots");
    this.observerPulseTracker =
        Objects.requireNonNull(observerPulseTracker, "observerPulseTracker");
    this.tick = tick;
  }

  public ShadowWorld shadowWorld() {
    return shadowWorld;
  }

  public ContainerSnapshotRegistry containerSnapshots() {
    return containerSnapshots;
  }

  public ObserverPulseTracker observerPulseTracker() {
    return observerPulseTracker;
  }

  public long tick() {
    return tick;
  }

  /**
   * Creates a world access view using {@code sourcePos} as the implicit neighbour update source.
   */
  public ComponentWorldAccess createWorldAccess(BlockPos sourcePos) {
    return new ComponentWorldAccess(shadowWorld, containerSnapshots, sourcePos);
  }
}
