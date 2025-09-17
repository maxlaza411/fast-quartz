package dev.fastquartz.fastquartz.engine.shadow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

class ShadowWorldTest {
  private static final BlockState AIR = BlockState.of("minecraft:air");
  private static final BlockState STONE = BlockState.of("minecraft:stone");
  private static final BlockState DIRT = BlockState.of("minecraft:dirt");
  private static final BlockState GOLD = BlockState.of("minecraft:gold_block");

  @Test
  void commitAppliesAllStagedChangesAtomically() {
    FakeServerWorld world = new FakeServerWorld();
    BlockPos first = new BlockPos(0, 64, 0);
    BlockPos second = new BlockPos(1, 64, 0);
    world.prime(first, AIR);
    world.prime(second, AIR);

    ShadowWorld shadow = new ShadowWorld(world);
    shadow.set(first, STONE, null);
    shadow.set(second, DIRT, null);

    assertEquals(AIR, world.getBlockState(first));
    assertEquals(STONE, shadow.get(first));

    ShadowCommitStats stats = shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS);
    assertEquals(2, stats.blocksApplied());
    assertTrue(stats.neighborChanges().isEmpty());
    assertEquals(STONE, world.getBlockState(first));
    assertEquals(DIRT, world.getBlockState(second));

    ShadowCommitStats repeat = shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS);
    assertEquals(0, repeat.blocksApplied());
    assertTrue(repeat.neighborChanges().isEmpty());
  }

  @Test
  void neighborCaptureQueuesWithoutVanillaRecursion() {
    FakeServerWorld world = new FakeServerWorld();
    ShadowWorld shadow = new ShadowWorld(world);
    BlockPos origin = new BlockPos(0, 70, 0);
    BlockPos neighbor = origin.offset(Direction.NORTH);

    shadow.set(origin, STONE, null);
    shadow.captureNeighbor(origin, Direction.NORTH);

    ShadowCommitStats stats = shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS);
    assertEquals(1, stats.blocksApplied());
    assertEquals(List.of(neighbor), stats.neighborChanges());
    assertEquals(World.NO_NEIGHBOR_UPDATES | World.NO_LIGHTING, world.lastFlags());
  }

  @Test
  void rollbackRestoresPreviousStateOnFailure() {
    FakeServerWorld world = new FakeServerWorld();
    BlockPos first = new BlockPos(4, 65, 4);
    BlockPos second = new BlockPos(5, 65, 4);
    world.prime(first, AIR);
    world.prime(second, AIR);

    ShadowWorld shadow = new ShadowWorld(world);
    shadow.set(first, STONE, null);
    shadow.set(second, DIRT, null);
    world.failOn(second);

    assertThrows(
        RuntimeException.class, () -> shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS));
    assertEquals(AIR, world.getBlockState(first));
    assertEquals(AIR, world.getBlockState(second));

    ShadowCommitStats stats = shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS);
    assertEquals(0, stats.blocksApplied());
    assertTrue(stats.neighborChanges().isEmpty());
  }

  @Test
  void commitPropagatesBlockEntityDataWithCopySemantics() {
    FakeServerWorld world = new FakeServerWorld();
    ShadowWorld shadow = new ShadowWorld(world);
    BlockPos beacon = new BlockPos(8, 72, 8);

    NbtCompound nbt = new NbtCompound();
    nbt.put("level", 5);
    shadow.set(beacon, GOLD, nbt);
    nbt.put("level", 1); // mutate after staging; shadow should keep the original copy.

    ShadowCommitStats stats = shadow.commit(world, CommitPolicy.SILENT_NO_PHYSICS);
    assertEquals(1, stats.blocksApplied());
    assertTrue(stats.neighborChanges().isEmpty());

    NbtCompound stored = world.blockEntity(beacon);
    assertNotNull(stored);
    assertEquals(5, stored.get("level"));
  }

  private static final class FakeServerWorld extends ServerWorld {
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private final Map<BlockPos, NbtCompound> blockEntities = new HashMap<>();
    private BlockPos failOn;
    private int lastFlags = 0;

    @Override
    public BlockState getBlockState(BlockPos pos) {
      return this.states.getOrDefault(pos, AIR);
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
      if (pos.equals(this.failOn)) {
        throw new RuntimeException("Injected failure");
      }
      this.lastFlags = flags;
      this.states.put(pos, state);
      return true;
    }

    @Override
    public NbtCompound getBlockEntityNbt(BlockPos pos) {
      NbtCompound nbt = this.blockEntities.get(pos);
      return nbt == null ? null : nbt.copy();
    }

    @Override
    public void setBlockEntityNbt(BlockPos pos, NbtCompound nbt) {
      this.blockEntities.put(pos, nbt.copy());
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
      this.blockEntities.remove(pos);
    }

    void prime(BlockPos pos, BlockState state) {
      this.states.put(pos, state);
    }

    void failOn(BlockPos pos) {
      this.failOn = pos;
    }

    int lastFlags() {
      return this.lastFlags;
    }

    NbtCompound blockEntity(BlockPos pos) {
      return this.blockEntities.get(pos);
    }
  }
}
