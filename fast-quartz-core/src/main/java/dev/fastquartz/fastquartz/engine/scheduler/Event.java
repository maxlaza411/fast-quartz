package dev.fastquartz.fastquartz.engine.scheduler;

import dev.fastquartz.fastquartz.engine.BlockPos;
import java.util.Objects;

/** Compact event payload describing simulation work. */
public final class Event {
  private final EventKey key;
  private final EvType type;
  private final BlockPos pos;
  private final int aux;
  private final @Nullable Object data;

  public Event(EventKey key, EvType type, BlockPos pos, int aux, @Nullable Object data) {
    this.key = Objects.requireNonNull(key, "key");
    this.type = Objects.requireNonNull(type, "type");
    this.pos = Objects.requireNonNull(pos, "pos");
    this.aux = aux;
    this.data = data;
  }

  public EventKey key() {
    return this.key;
  }

  public EvType type() {
    return this.type;
  }

  public BlockPos pos() {
    return this.pos;
  }

  public int aux() {
    return this.aux;
  }

  public @Nullable Object data() {
    return this.data;
  }

  @Override
  public String toString() {
    return "Event{"
        + "key="
        + this.key
        + ", type="
        + this.type
        + ", pos="
        + this.pos
        + ", aux="
        + this.aux
        + '}';
  }
}
