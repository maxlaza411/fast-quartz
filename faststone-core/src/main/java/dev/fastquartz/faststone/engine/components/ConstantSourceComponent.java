package dev.fastquartz.faststone.engine.components;

import dev.fastquartz.faststone.engine.BlockPos;
import dev.fastquartz.faststone.engine.EvaluationContext;
import dev.fastquartz.faststone.engine.RedstoneComponent;
import dev.fastquartz.faststone.engine.RedstoneMath;

/** Simple component that always emits the configured power level. */
public final class ConstantSourceComponent implements RedstoneComponent {
  private int output;

  public ConstantSourceComponent(int output) {
    this.output = RedstoneMath.requirePowerRange(output, "output");
  }

  public int output() {
    return output;
  }

  public void setOutput(int output) {
    this.output = RedstoneMath.requirePowerRange(output, "output");
  }

  @Override
  public int evaluate(BlockPos position, EvaluationContext context) {
    return output;
  }
}
