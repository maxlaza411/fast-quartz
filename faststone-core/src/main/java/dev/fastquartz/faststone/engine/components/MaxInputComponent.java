package dev.fastquartz.faststone.engine.components;

import dev.fastquartz.faststone.engine.BlockPos;
import dev.fastquartz.faststone.engine.EvaluationContext;
import dev.fastquartz.faststone.engine.RedstoneComponent;
import dev.fastquartz.faststone.engine.RedstoneMath;

/** Component that mirrors the strongest incoming power, optionally with a delay. */
public final class MaxInputComponent implements RedstoneComponent {
  private final int delayTicks;

  public MaxInputComponent(int delayTicks) {
    if (delayTicks < 0) {
      throw new IllegalArgumentException("delayTicks must be non-negative");
    }
    this.delayTicks = delayTicks;
  }

  @Override
  public int evaluate(BlockPos position, EvaluationContext context) {
    int strongest = RedstoneMath.MIN_POWER;
    for (BlockPos input : context.inputsOf(position)) {
      strongest = Math.max(strongest, context.powerAt(input));
    }
    return strongest;
  }

  @Override
  public int propagationDelayTicks() {
    return delayTicks;
  }
}
