package dev.fastquartz.fastquartz.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/** Deterministic discrete-event simulation core for redstone networks. */
public final class SimulationEngine {
  private final RedstoneNetwork network;
  private final EvaluationContext context = new EngineEvaluationContext();
  private final PriorityQueue<ScheduledUpdate> queue;
  private long currentTick;
  private long nextSequence;

  public SimulationEngine(RedstoneNetwork network) {
    this.network = Objects.requireNonNull(network, "network");
    this.queue =
        new PriorityQueue<>(
            Comparator.comparingLong(ScheduledUpdate::tick)
                .thenComparing(ScheduledUpdate::position)
                .thenComparingLong(ScheduledUpdate::sequence));
    for (BlockPos position : network.positions()) {
      enqueue(position, currentTick);
    }
  }

  /** Returns the tick the engine has progressed to. */
  public long currentTick() {
    return currentTick;
  }

  /** Marks a component as needing re-evaluation due to an input change. */
  public void markDirty(BlockPos position) {
    markDirty(position, 0L);
  }

  /** Marks a component as dirty and schedules it after an additional delay. */
  public void markDirty(BlockPos position, long extraDelayTicks) {
    if (extraDelayTicks < 0) {
      throw new IllegalArgumentException("extraDelayTicks must be non-negative");
    }
    RedstoneNetwork.Node node = network.requireNode(position);
    long dueTick = computeDueTick(node, currentTick + extraDelayTicks);
    enqueue(position, dueTick);
  }

  /** Advances the simulation by {@code ticks}, returning the observed state changes. */
  public ElapseResult elapse(long ticks) {
    if (ticks < 0) {
      throw new IllegalArgumentException("ticks must be non-negative");
    }
    long startTick = currentTick;
    long targetTick = currentTick + ticks;
    List<StateChange> changes = new ArrayList<>();
    while (!queue.isEmpty() && queue.peek().tick <= targetTick) {
      ScheduledUpdate update = queue.poll();
      if (update.tick < currentTick) {
        continue;
      }
      if (update.tick > currentTick) {
        currentTick = update.tick;
      }
      RedstoneNetwork.Node node = network.nodeIfPresent(update.position);
      if (node == null) {
        continue;
      }
      int computed = RedstoneMath.clampPower(node.component.evaluate(update.position, context));
      if (computed != node.power) {
        int previous = node.power;
        node.power = computed;
        changes.add(new StateChange(currentTick, update.position, previous, computed));
        scheduleOutputs(node);
      }
    }
    currentTick = targetTick;
    return new ElapseResult(startTick, currentTick, changes);
  }

  private void scheduleOutputs(RedstoneNetwork.Node node) {
    for (BlockPos target : node.outputs) {
      RedstoneNetwork.Node targetNode = network.nodeIfPresent(target);
      if (targetNode == null) {
        continue;
      }
      long dueTick = computeDueTick(targetNode, currentTick);
      enqueue(target, dueTick);
    }
  }

  private void enqueue(BlockPos position, long tick) {
    queue.add(new ScheduledUpdate(tick, position, nextSequence++));
  }

  private long computeDueTick(RedstoneNetwork.Node node, long baseTick) {
    long delay = Math.max(0L, node.component.propagationDelayTicks());
    return baseTick + delay;
  }

  private final class EngineEvaluationContext implements EvaluationContext {
    @Override
    public int powerAt(BlockPos position) {
      RedstoneNetwork.Node node = network.nodeIfPresent(position);
      if (node == null) {
        return RedstoneMath.MIN_POWER;
      }
      return node.power;
    }

    @Override
    public Collection<BlockPos> inputsOf(BlockPos position) {
      RedstoneNetwork.Node node = network.nodeIfPresent(position);
      if (node == null) {
        return List.of();
      }
      return List.copyOf(node.inputs);
    }
  }

  private record ScheduledUpdate(long tick, BlockPos position, long sequence) {}
}
