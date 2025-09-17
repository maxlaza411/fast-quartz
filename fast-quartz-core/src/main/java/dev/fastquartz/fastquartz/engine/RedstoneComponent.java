package dev.fastquartz.fastquartz.engine;

/** Behaviour contract for a node participating in the redstone network graph. */
public interface RedstoneComponent {
  /** Computes the new power level for this component given the current network view. */
  int evaluate(BlockPos position, EvaluationContext context);

  /**
   * Returns the number of ticks this component waits before applying an input change to its
   * outputs. Defaults to 0 (immediate effect within the same tick solve).
   */
  default int propagationDelayTicks() {
    return 0;
  }
}
