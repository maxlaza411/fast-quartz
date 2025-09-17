package dev.fastquartz.fastquartz.engine;

import java.util.Collection;

/** Provides read-only access to the network when evaluating a component. */
public interface EvaluationContext {
  /** Returns the current power level at the given position (0-15). */
  int powerAt(BlockPos position);

  /** Returns the upstream positions feeding the given position. */
  Collection<BlockPos> inputsOf(BlockPos position);
}
