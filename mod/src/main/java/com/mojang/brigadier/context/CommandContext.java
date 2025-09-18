package com.mojang.brigadier.context;

/** Minimal stub of Brigadier's {@code CommandContext}. */
public final class CommandContext<S> {
  private final S source;

  public CommandContext(S source) {
    this.source = source;
  }

  public S getSource() {
    return source;
  }
}
