package dev.fastquartz.engine.component;

import dev.fastquartz.engine.world.BlockPos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Factory for deterministic wrappers around vanilla component logic. */
public final class ComponentAdapters {
  private static final Comparator<BlockPos> BLOCK_POS_ORDER =
      Comparator.comparingInt(BlockPos::y)
          .thenComparingInt(BlockPos::z)
          .thenComparingInt(BlockPos::x);

  private static final PreRunHook NO_OP =
      new PreRunHook() {
        @Override
        public void run(ComponentWorldAccess world, BlockPos pos, int stateBits) {
          // no-op
        }
      };

  private ComponentAdapters() {}

  /** Adapter used for torches, repeaters and other simple stateful components. */
  public static ComponentAdapter standard(ComponentLogic logic) {
    Objects.requireNonNull(logic, "logic");
    return new StandardComponentAdapter(logic, NO_OP);
  }

  /** Adapter specialised for comparators that require container snapshots. */
  public static ComponentAdapter comparator(
      ComponentLogic logic, ComparatorSnapshotLocator snapshotLocator) {
    Objects.requireNonNull(logic, "logic");
    Objects.requireNonNull(snapshotLocator, "snapshotLocator");
    return new StandardComponentAdapter(logic, new ComparatorPreRun(snapshotLocator));
  }

  /** Adapter wrapping observer behaviour with Stable-logic pulse coalescing. */
  public static ObserverAdapter observer(ObserverLogic logic) {
    Objects.requireNonNull(logic, "logic");
    return new DefaultObserverAdapter(logic);
  }

  /** Adapter wrapping piston transactions. */
  public static PistonAdapter piston(PistonLogic logic) {
    Objects.requireNonNull(logic, "logic");
    return new DefaultPistonAdapter(logic);
  }

  @FunctionalInterface
  public interface ComponentAdapter {
    void apply(ComponentContext context, BlockPos componentPos, int stateBits);
  }

  @FunctionalInterface
  public interface ObserverAdapter {
    void apply(
        ComponentContext context,
        BlockPos observerPos,
        BlockPos observedPos,
        int oldStateBits,
        int newStateBits);
  }

  @FunctionalInterface
  public interface PistonAdapter {
    void apply(ComponentContext context, BlockPos componentPos, int stateBits);
  }

  @FunctionalInterface
  public interface ComponentLogic {
    void run(ComponentWorldAccess world, BlockPos pos, int stateBits);
  }

  @FunctionalInterface
  public interface ObserverLogic {
    void run(
        ComponentWorldAccess world,
        BlockPos observerPos,
        BlockPos observedPos,
        int oldStateBits,
        int newStateBits);
  }

  @FunctionalInterface
  public interface PistonLogic {
    PistonTransaction evaluate(ComponentWorldAccess world, BlockPos pos, int stateBits);
  }

  @FunctionalInterface
  public interface ComparatorSnapshotLocator {
    Iterable<BlockPos> snapshotPositions(BlockPos comparatorPos, int stateBits);
  }

  public static final class PistonTransaction {
    private final List<PistonMove> moves;
    private final List<BlockPos> neighborUpdates;

    public PistonTransaction(List<PistonMove> moves, List<BlockPos> neighborUpdates) {
      this.moves = List.copyOf(Objects.requireNonNull(moves, "moves"));
      this.neighborUpdates = neighborUpdates == null ? List.of() : List.copyOf(neighborUpdates);
    }

    public List<PistonMove> moves() {
      return moves;
    }

    public List<BlockPos> neighborUpdates() {
      return neighborUpdates;
    }
  }

  public record PistonMove(BlockPos from, BlockPos to, int stateBits) {
    public PistonMove {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
    }
  }

  private record ComparatorPreRun(ComparatorSnapshotLocator locator) implements PreRunHook {
    @Override
    public void run(ComponentWorldAccess world, BlockPos pos, int stateBits) {
      Iterable<BlockPos> positions = locator.snapshotPositions(pos, stateBits);
      if (positions == null) {
        return;
      }
      for (BlockPos snapshotPos : positions) {
        if (snapshotPos != null) {
          world.readContainerSignal(snapshotPos);
        }
      }
    }
  }

  private interface PreRunHook {
    void run(ComponentWorldAccess world, BlockPos pos, int stateBits);
  }

  private static final class StandardComponentAdapter implements ComponentAdapter {
    private final ComponentLogic logic;
    private final PreRunHook preRun;

    StandardComponentAdapter(ComponentLogic logic, PreRunHook preRun) {
      this.logic = Objects.requireNonNull(logic, "logic");
      this.preRun = Objects.requireNonNull(preRun, "preRun");
    }

    @Override
    public void apply(ComponentContext context, BlockPos componentPos, int stateBits) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(componentPos, "componentPos");
      ComponentWorldAccess world = context.createWorldAccess(componentPos);
      preRun.run(world, componentPos, stateBits);
      logic.run(world, componentPos, stateBits);
    }
  }

  private static final class DefaultObserverAdapter implements ObserverAdapter {
    private final ObserverLogic logic;

    DefaultObserverAdapter(ObserverLogic logic) {
      this.logic = Objects.requireNonNull(logic, "logic");
    }

    @Override
    public void apply(
        ComponentContext context,
        BlockPos observerPos,
        BlockPos observedPos,
        int oldStateBits,
        int newStateBits) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(observerPos, "observerPos");
      Objects.requireNonNull(observedPos, "observedPos");
      if (oldStateBits == newStateBits) {
        return;
      }
      if (!context.observerPulseTracker().shouldEmit(context.tick(), observerPos, observedPos)) {
        return;
      }
      ComponentWorldAccess world = context.createWorldAccess(observerPos);
      logic.run(world, observerPos, observedPos, oldStateBits, newStateBits);
    }
  }

  private static final class DefaultPistonAdapter implements PistonAdapter {
    private final PistonLogic logic;

    DefaultPistonAdapter(PistonLogic logic) {
      this.logic = Objects.requireNonNull(logic, "logic");
    }

    @Override
    public void apply(ComponentContext context, BlockPos componentPos, int stateBits) {
      Objects.requireNonNull(context, "context");
      Objects.requireNonNull(componentPos, "componentPos");
      ComponentWorldAccess world = context.createWorldAccess(componentPos);
      PistonTransaction transaction = logic.evaluate(world, componentPos, stateBits);
      Objects.requireNonNull(transaction, "transaction");

      List<PistonMove> orderedMoves = new ArrayList<>(transaction.moves());
      orderedMoves.sort((a, b) -> compareBlockPos(a.from(), b.from()));

      Set<BlockPos> seenFrom = new HashSet<>();
      Set<BlockPos> seenTo = new HashSet<>();
      for (PistonMove move : orderedMoves) {
        if (!seenFrom.add(move.from())) {
          throw new IllegalArgumentException("Duplicate piston source: " + move.from());
        }
        if (!seenTo.add(move.to())) {
          throw new IllegalArgumentException("Duplicate piston destination: " + move.to());
        }
      }

      for (PistonMove move : orderedMoves) {
        world.setBlockStateBits(move.from(), 0);
      }
      for (PistonMove move : orderedMoves) {
        world.setBlockStateBits(move.to(), move.stateBits());
      }

      TreeSet<BlockPos> neighbours = new TreeSet<>(BLOCK_POS_ORDER);
      for (PistonMove move : orderedMoves) {
        neighbours.add(move.from());
        neighbours.add(move.to());
      }
      neighbours.addAll(transaction.neighborUpdates());
      for (BlockPos neighbour : neighbours) {
        world.markNeighborChanged(neighbour, componentPos);
      }
    }
  }

  private static int compareBlockPos(BlockPos a, BlockPos b) {
    return BLOCK_POS_ORDER.compare(a, b);
  }
}
