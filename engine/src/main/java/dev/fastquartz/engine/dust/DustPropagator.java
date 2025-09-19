package dev.fastquartz.engine.dust;

import java.util.List;
import java.util.Objects;

/**
 * Interface for dust power propagation backends. Implementations settle the compressed dust network
 * to a deterministic fixed point when component sources change.
 */
public interface DustPropagator {

  /** Maximum power level a dust node can hold. */
  int MAX_POWER_LEVEL = 15;

  /**
   * Resets the propagator to operate on the supplied compressed graph. All power state is cleared
   * to zero.
   */
  void reset(DustCsrGraph graph);

  /**
   * Applies the supplied source updates and settles the dust network to a fixed point.
   *
   * <p>Each source identifies a dust node by id together with the power level emitted by
   * components attached to that node. Implementations may aggregate additional component inputs or
   * cached state; callers need only provide the nodes whose component-facing power changed.
   *
   * @param changedSources collection of source updates (node id, level)
   * @return array of node identifiers whose resolved power level changed as a result of the settle
   */
  int[] propagate(List<Source> changedSources);

  /** Returns the settled power level for the specified node. */
  int powerLevel(int nodeId);

  /** Immutable update describing the component power injected into a dust node. */
  record Source(int nodeId, int powerLevel) {
    public Source {
      if (nodeId < 0) {
        throw new IllegalArgumentException("nodeId must be non-negative");
      }
      if (powerLevel < 0 || powerLevel > MAX_POWER_LEVEL) {
        throw new IllegalArgumentException("powerLevel must be in [0, 15]");
      }
    }

    public static Source of(int nodeId, int powerLevel) {
      return new Source(nodeId, powerLevel);
    }
  }

  /** Utility for validating a non-null source entry. */
  static Source requireNonNull(Source source) {
    return Objects.requireNonNull(source, "source");
  }
}
