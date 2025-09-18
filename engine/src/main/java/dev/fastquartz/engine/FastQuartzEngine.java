package dev.fastquartz.engine;

import java.time.Duration;
import java.util.Objects;

/**
 * Minimal skeleton for the Fast Quartz simulation engine.
 *
 * <p>The implementation is intentionally tiny; real logic will be layered on in future tickets. For
 * now we just track a target TPS budget and expose a few deterministic helpers used by tests and
 * integration scaffolding.
 */
public final class FastQuartzEngine {
  private final int targetTps;

  private FastQuartzEngine(int targetTps) {
    this.targetTps = targetTps;
  }

  /**
   * Creates a new engine instance with the provided tick rate.
   *
   * @param targetTps ticks per second, must be positive
   * @return engine configured for the requested tick rate
   */
  public static FastQuartzEngine create(int targetTps) {
    if (targetTps <= 0) {
      throw new IllegalArgumentException("targetTps must be positive");
    }
    return new FastQuartzEngine(targetTps);
  }

  public int targetTps() {
    return targetTps;
  }

  /** Returns how much wall-clock time a single tick represents. */
  public Duration tickDuration() {
    long nanos = Math.round(1_000_000_000.0 / targetTps);
    return Duration.ofNanos(nanos);
  }

  /**
   * Computes the number of ticks required to cover the supplied duration. The result is rounded to
   * the nearest tick in order to keep deterministic behavior for replay scenarios.
   */
  public long ticksFor(Duration duration) {
    Objects.requireNonNull(duration, "duration");
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must be non-negative");
    }
    double ticks = duration.toNanos() / (double) tickDuration().toNanos();
    return Math.round(ticks);
  }
}
