package net.minecraft.server.world;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Minimal stub for Minecraft's {@code ServerWorld}. */
public abstract class ServerWorld extends World {
  public abstract BlockState getBlockState(BlockPos pos);

  public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags);

  public NbtCompound getBlockEntityNbt(BlockPos pos) {
    return null;
  }

  public void setBlockEntityNbt(BlockPos pos, NbtCompound nbt) {}

  public void removeBlockEntity(BlockPos pos) {}
}
