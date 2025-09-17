package dev.fastquartz.fastquartz.engine.shadow;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Overlay view that buffers block mutations before atomically committing them. */
public interface Shadow {
  /** Returns the staged state at {@code pos}, falling back to the live world if untouched. */
  BlockState get(BlockPos pos);

  /** Stages a block state (and optional block-entity data) to be written on commit. */
  void set(BlockPos pos, BlockState state, NbtCompound blockEntityNbt);

  /** Records that {@code src} notified its neighbour along {@code dir}. */
  void captureNeighbor(BlockPos src, Direction dir);

  /** Drops all staged state without mutating the backing world. */
  void clear();

  /** Applies the staged state to {@code world} according to {@code policy}. */
  ShadowCommitStats commit(ServerWorld world, CommitPolicy policy);
}
