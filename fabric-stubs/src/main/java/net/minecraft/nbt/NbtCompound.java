package net.minecraft.nbt;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal mutable NBT compound supporting copy semantics for tests. */
public final class NbtCompound extends LinkedHashMap<String, Object> {
  private static final long serialVersionUID = 1L;
  public NbtCompound() {}

  private NbtCompound(Map<String, Object> backing) {
    super(backing);
  }

  /** Returns a shallow copy of this compound. */
  public NbtCompound copy() {
    return new NbtCompound(this);
  }
}
