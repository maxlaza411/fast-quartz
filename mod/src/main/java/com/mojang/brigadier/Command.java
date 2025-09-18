package com.mojang.brigadier;

import com.mojang.brigadier.context.CommandContext;

/** Minimal stub of Brigadier's {@code Command}. */
@FunctionalInterface
public interface Command<S> {
  int SINGLE_SUCCESS = 1;

  int run(CommandContext<S> context);
}
