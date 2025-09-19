package dev.fastquartz.engine.component;

import dev.fastquartz.engine.world.BlockPos;
import dev.fastquartz.engine.world.ShadowWorld;
import java.util.Objects;

/**
 * Minimal world facade exposed to vanilla component logic. All writes are staged in the associated
 * {@link ShadowWorld} overlay to preserve deterministic ordering.
 */
public final class ComponentWorldAccess {
  private final ShadowWorld shadowWorld;
  private final ContainerSnapshotRegistry containerSnapshots;
  private final BlockPos defaultSource;

  public ComponentWorldAccess(
      ShadowWorld shadowWorld,
      ContainerSnapshotRegistry containerSnapshots,
      BlockPos defaultSource) {
    this.shadowWorld = Objects.requireNonNull(shadowWorld, "shadowWorld");
    this.containerSnapshots = Objects.requireNonNull(containerSnapshots, "containerSnapshots");
    this.defaultSource = Objects.requireNonNull(defaultSource, "defaultSource");
  }

  public int getBlockStateBits(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    return shadowWorld.getBlockStateBits(pos);
  }

  public void setBlockStateBits(BlockPos pos, int stateBits) {
    Objects.requireNonNull(pos, "pos");
    shadowWorld.setBlockStateBits(pos, stateBits);
  }

  public void scheduleTick(BlockPos pos, int delayTicks, int priority) {
    Objects.requireNonNull(pos, "pos");
    shadowWorld.scheduleTick(pos, delayTicks, priority);
  }

  public void markNeighborChanged(BlockPos pos) {
    markNeighborChanged(pos, defaultSource);
  }

  public void markNeighborChanged(BlockPos pos, BlockPos source) {
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(source, "source");
    shadowWorld.markNeighborChanged(pos, source);
  }

  public int readContainerSignal(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    return containerSnapshots.snapshot(pos, () -> shadowWorld.readContainerSignal(pos));
  }

  public ShadowWorld shadowWorld() {
    return shadowWorld;
  }
}
