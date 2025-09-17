package dev.fastquartz.fastquartz.engine.shadow;

import java.util.List;
import java.util.Objects;
import net.minecraft.util.math.BlockPos;

/** Result produced by {@link Shadow#commit(ServerWorld, CommitPolicy)}. */
public final class ShadowCommitStats {
  private final int blocksApplied;
  private final List<BlockPos> neighborChanges;

  public ShadowCommitStats(int blocksApplied, List<BlockPos> neighborChanges) {
    this.blocksApplied = blocksApplied;
    this.neighborChanges = List.copyOf(Objects.requireNonNull(neighborChanges, "neighborChanges"));
  }

  public int blocksApplied() {
    return this.blocksApplied;
  }

  public List<BlockPos> neighborChanges() {
    return this.neighborChanges;
  }
}
