package dev.fastquartz.fastquartz.engine.shadow;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Sparse write log that overlays block mutations on top of a live world. */
public final class ShadowWorld implements Shadow {
  private static final int SILENT_FLAGS = World.NO_NEIGHBOR_UPDATES | World.NO_LIGHTING;
  private static final int SECTION_SIZE = 16;
  private static final int LOCAL_MASK = SECTION_SIZE - 1;
  private static final int SECTION_Y_BITS = 20;
  private static final int CHUNK_COORD_BITS = 22;
  private static final int CHUNK_Z_SHIFT = SECTION_Y_BITS;
  private static final int CHUNK_X_SHIFT = CHUNK_Z_SHIFT + CHUNK_COORD_BITS;
  private static final long SECTION_Y_MASK = (1L << SECTION_Y_BITS) - 1L;
  private static final long CHUNK_COORD_MASK = (1L << CHUNK_COORD_BITS) - 1L;

  private final ServerWorld world;
  private final Long2ObjectOpenHashMap<SectionDelta> deltas = new Long2ObjectOpenHashMap<>();
  private final ObjectArrayList<BlockPos> neighborQueue = new ObjectArrayList<>();
  private final BitSet touchedChunks = new BitSet();
  private final LongArrayList touchedChunkKeys = new LongArrayList();
  private final Long2IntOpenHashMap chunkIndex = new Long2IntOpenHashMap();

  public ShadowWorld(ServerWorld world) {
    this.world = Objects.requireNonNull(world, "world");
    this.chunkIndex.defaultReturnValue(-1);
  }

  @Override
  public BlockState get(BlockPos pos) {
    Objects.requireNonNull(pos, "pos");
    long sectionKey = packSectionKey(pos);
    SectionDelta delta = this.deltas.get(sectionKey);
    if (delta != null) {
      short localIndex = computeLocalIndex(pos);
      BlockState staged = delta.get(localIndex);
      if (staged != null) {
        return staged;
      }
    }
    return this.world.getBlockState(pos);
  }

  @Override
  public void set(BlockPos pos, BlockState state, NbtCompound blockEntityNbt) {
    Objects.requireNonNull(pos, "pos");
    Objects.requireNonNull(state, "state");
    long sectionKey = packSectionKey(pos);
    SectionDelta delta = this.deltas.computeIfAbsent(sectionKey, ignored -> new SectionDelta());
    short localIndex = computeLocalIndex(pos);
    NbtCompound nbtCopy = blockEntityNbt != null ? blockEntityNbt.copy() : null;
    delta.put(localIndex, state, nbtCopy);

    int chunkX = pos.getX() >> 4;
    int chunkZ = pos.getZ() >> 4;
    markChunkTouched(chunkX, chunkZ);
  }

  @Override
  public void captureNeighbor(BlockPos src, Direction dir) {
    Objects.requireNonNull(src, "src");
    Objects.requireNonNull(dir, "dir");
    this.neighborQueue.add(src.offset(dir));
  }

  @Override
  public void clear() {
    this.deltas.clear();
    this.neighborQueue.clear();
    this.touchedChunks.clear();
    this.touchedChunkKeys.clear();
    this.chunkIndex.clear();
    this.chunkIndex.defaultReturnValue(-1);
  }

  @Override
  public ShadowCommitStats commit(ServerWorld world, CommitPolicy policy) {
    Objects.requireNonNull(world, "world");
    Objects.requireNonNull(policy, "policy");
    if (world != this.world) {
      throw new IllegalArgumentException("commit world does not match shadow base world");
    }
    if (policy != CommitPolicy.SILENT_NO_PHYSICS) {
      throw new UnsupportedOperationException("Unsupported commit policy: " + policy);
    }

    List<BlockMutation> mutations = collectMutations(world);
    List<BlockPos> neighborsSnapshot = List.copyOf(this.neighborQueue);
    if (mutations.isEmpty() && neighborsSnapshot.isEmpty()) {
      clear();
      return new ShadowCommitStats(0, neighborsSnapshot);
    }

    List<BlockMutation> applied = new ArrayList<>(mutations.size());
    try {
      for (BlockMutation mutation : mutations) {
        world.setBlockState(mutation.pos, mutation.newState, SILENT_FLAGS);
        if (mutation.newBlockEntityNbt != null) {
          world.setBlockEntityNbt(mutation.pos, mutation.newBlockEntityNbt.copy());
        } else if (mutation.previousBlockEntityNbt != null) {
          world.removeBlockEntity(mutation.pos);
        }
        applied.add(mutation);
      }
    } catch (RuntimeException failure) {
      rollback(world, applied);
      clear();
      throw failure;
    }

    clear();
    return new ShadowCommitStats(applied.size(), neighborsSnapshot);
  }

  private List<BlockMutation> collectMutations(ServerWorld world) {
    ObjectList<BlockMutation> mutations = new ObjectArrayList<>();
    for (Long2ObjectMap.Entry<SectionDelta> entry : this.deltas.long2ObjectEntrySet()) {
      long sectionKey = entry.getLongKey();
      SectionDelta delta = entry.getValue();
      int chunkX = unpackChunkX(sectionKey);
      int sectionY = unpackSectionY(sectionKey);
      int chunkZ = unpackChunkZ(sectionKey);
      for (int i = 0; i < delta.size(); i++) {
        short localIndex = delta.indexAt(i);
        BlockPos pos = toWorldPos(chunkX, sectionY, chunkZ, localIndex);
        BlockState newState = delta.stateAt(i);
        BlockState previousState = world.getBlockState(pos);
        NbtCompound previousNbt = world.getBlockEntityNbt(pos);
        if (previousNbt != null) {
          previousNbt = previousNbt.copy();
        }
        NbtCompound stagedNbt = delta.blockEntityNbtAt(i);
        if (stagedNbt != null) {
          stagedNbt = stagedNbt.copy();
        }
        mutations.add(new BlockMutation(pos, previousState, previousNbt, newState, stagedNbt));
      }
    }
    return mutations;
  }

  private void rollback(ServerWorld world, List<BlockMutation> applied) {
    for (int i = applied.size() - 1; i >= 0; i--) {
      BlockMutation mutation = applied.get(i);
      world.setBlockState(mutation.pos, mutation.previousState, SILENT_FLAGS);
      if (mutation.previousBlockEntityNbt != null) {
        world.setBlockEntityNbt(mutation.pos, mutation.previousBlockEntityNbt.copy());
      } else {
        world.removeBlockEntity(mutation.pos);
      }
    }
  }

  private void markChunkTouched(int chunkX, int chunkZ) {
    long chunkKey = packChunkKey(chunkX, chunkZ);
    int slot = this.chunkIndex.get(chunkKey);
    if (slot < 0) {
      slot = this.touchedChunkKeys.size();
      this.chunkIndex.put(chunkKey, slot);
      this.touchedChunkKeys.add(chunkKey);
    }
    this.touchedChunks.set(slot);
  }

  private static BlockPos toWorldPos(int chunkX, int sectionY, int chunkZ, short localIndex) {
    int localX = localIndex & LOCAL_MASK;
    int localZ = (localIndex >>> 4) & LOCAL_MASK;
    int localY = (localIndex >>> 8) & LOCAL_MASK;
    int x = (chunkX << 4) | localX;
    int y = (sectionY << 4) | localY;
    int z = (chunkZ << 4) | localZ;
    return new BlockPos(x, y, z);
  }

  private static short computeLocalIndex(BlockPos pos) {
    int localX = pos.getX() & LOCAL_MASK;
    int localY = pos.getY() & LOCAL_MASK;
    int localZ = pos.getZ() & LOCAL_MASK;
    return (short) ((localY << 8) | (localZ << 4) | localX);
  }

  private static long packSectionKey(BlockPos pos) {
    int chunkX = pos.getX() >> 4;
    int chunkZ = pos.getZ() >> 4;
    int sectionY = pos.getY() >> 4;
    return packSectionKey(chunkX, chunkZ, sectionY);
  }

  private static long packSectionKey(int chunkX, int chunkZ, int sectionY) {
    long packedX = asUnsigned(chunkX, CHUNK_COORD_BITS) << CHUNK_X_SHIFT;
    long packedZ = asUnsigned(chunkZ, CHUNK_COORD_BITS) << CHUNK_Z_SHIFT;
    long packedY = asUnsigned(sectionY, SECTION_Y_BITS);
    return packedX | packedZ | packedY;
  }

  private static long packChunkKey(int chunkX, int chunkZ) {
    return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
  }

  private static long asUnsigned(int value, int bits) {
    long mask = (1L << bits) - 1L;
    return ((long) value) & mask;
  }

  private static int unpackChunkX(long sectionKey) {
    long value = (sectionKey >> CHUNK_X_SHIFT) & CHUNK_COORD_MASK;
    return decodeSigned(value, CHUNK_COORD_BITS);
  }

  private static int unpackChunkZ(long sectionKey) {
    long value = (sectionKey >> CHUNK_Z_SHIFT) & CHUNK_COORD_MASK;
    return decodeSigned(value, CHUNK_COORD_BITS);
  }

  private static int unpackSectionY(long sectionKey) {
    long value = sectionKey & SECTION_Y_MASK;
    return decodeSigned(value, SECTION_Y_BITS);
  }

  private static int decodeSigned(long value, int bits) {
    long shift = 64L - bits;
    return (int) ((value << shift) >> shift);
  }

  private record BlockMutation(
      BlockPos pos,
      BlockState previousState,
      NbtCompound previousBlockEntityNbt,
      BlockState newState,
      NbtCompound newBlockEntityNbt) {}
}
