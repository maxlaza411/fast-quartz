package dev.fastquartz.fastquartz.engine.shadow;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;

/** Sparse mutation list for a chunk section (16×16×16 blocks). */
final class SectionDelta {
  final ShortArrayList indices = new ShortArrayList();
  final ObjectArrayList<BlockState> states = new ObjectArrayList<>();
  final ObjectArrayList<NbtCompound> blockEntityNbt = new ObjectArrayList<>();

  int size() {
    return this.indices.size();
  }

  short indexAt(int slot) {
    return this.indices.getShort(slot);
  }

  BlockState stateAt(int slot) {
    return this.states.get(slot);
  }

  NbtCompound blockEntityNbtAt(int slot) {
    return this.blockEntityNbt.get(slot);
  }

  void put(short localIndex, BlockState state, NbtCompound nbt) {
    int slot = findSlot(localIndex);
    if (slot >= 0) {
      this.states.set(slot, state);
      this.blockEntityNbt.set(slot, nbt);
      return;
    }
    this.indices.add(localIndex);
    this.states.add(state);
    this.blockEntityNbt.add(nbt);
  }

  BlockState get(short localIndex) {
    int slot = findSlot(localIndex);
    if (slot < 0) {
      return null;
    }
    return this.states.get(slot);
  }

  NbtCompound getBlockEntityNbt(short localIndex) {
    int slot = findSlot(localIndex);
    if (slot < 0) {
      return null;
    }
    return this.blockEntityNbt.get(slot);
  }

  private int findSlot(short localIndex) {
    for (int i = 0; i < this.indices.size(); i++) {
      if (this.indices.getShort(i) == localIndex) {
        return i;
      }
    }
    return -1;
  }
}
