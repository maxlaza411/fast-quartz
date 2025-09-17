package dev.fastquartz.fastquartz.engine.components;

import dev.fastquartz.fastquartz.engine.BlockPos;
import dev.fastquartz.fastquartz.engine.EvaluationContext;
import dev.fastquartz.fastquartz.engine.RedstoneComponent;
import dev.fastquartz.fastquartz.engine.RedstoneMath;

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
