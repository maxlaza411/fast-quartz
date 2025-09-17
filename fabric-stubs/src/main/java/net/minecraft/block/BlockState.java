package net.minecraft.block;

import java.util.Objects;

/** Simplified representation of a block state for unit testing. */
public final class BlockState {
  private final String id;

  public BlockState(String id) {
    this.id = Objects.requireNonNull(id, "id");
  }

  public String getId() {
    return this.id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockState other)) {
      return false;
    }
    return this.id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public String toString() {
    return "BlockState{" + this.id + '}';
  }

  /** Convenience factory mirroring the real game's helper methods. */
  public static BlockState of(String id) {
    return new BlockState(id);
  }
}
