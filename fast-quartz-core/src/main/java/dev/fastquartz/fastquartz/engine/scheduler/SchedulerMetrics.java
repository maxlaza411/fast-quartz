package dev.fastquartz.fastquartz.engine.scheduler;

import java.util.concurrent.atomic.LongAdder;

/** Aggregated scheduler metrics for operations and latency accounting. */
public final class SchedulerMetrics {
  private final LongAdder scheduleAtTickOps = new LongAdder();
  private final LongAdder scheduleAtTickLatencyNanos = new LongAdder();
  private final LongAdder scheduleMicroOps = new LongAdder();
  private final LongAdder scheduleMicroLatencyNanos = new LongAdder();
  private final LongAdder offerMicroOps = new LongAdder();
  private final LongAdder offerMicroLatencyNanos = new LongAdder();
  private final LongAdder pollMicroOps = new LongAdder();
  private final LongAdder pollMicroLatencyNanos = new LongAdder();
  private final LongAdder drainTickOps = new LongAdder();
  private final LongAdder drainTickLatencyNanos = new LongAdder();
  private final LongAdder cancelOps = new LongAdder();
  private final LongAdder cancelLatencyNanos = new LongAdder();

  long startTimer() {
    return System.nanoTime();
  }

  void recordScheduleAtTick(long startNanos) {
    record(scheduleAtTickOps, scheduleAtTickLatencyNanos, startNanos);
  }

  void recordScheduleMicro(long startNanos) {
    record(scheduleMicroOps, scheduleMicroLatencyNanos, startNanos);
  }

  void recordOfferMicro(long startNanos) {
    record(offerMicroOps, offerMicroLatencyNanos, startNanos);
  }

  void recordPollMicro(long startNanos) {
    record(pollMicroOps, pollMicroLatencyNanos, startNanos);
  }

  void recordDrainTick(long startNanos) {
    record(drainTickOps, drainTickLatencyNanos, startNanos);
  }

  void recordCancel(long startNanos) {
    record(cancelOps, cancelLatencyNanos, startNanos);
  }

  private static void record(LongAdder ops, LongAdder nanos, long startNanos) {
    ops.increment();
    nanos.add(System.nanoTime() - startNanos);
  }

  public long scheduleAtTickOps() {
    return scheduleAtTickOps.sum();
  }

  public long scheduleAtTickLatencyNanos() {
    return scheduleAtTickLatencyNanos.sum();
  }

  public double scheduleAtTickAverageMicros() {
    return averageMicros(scheduleAtTickLatencyNanos.sum(), scheduleAtTickOps.sum());
  }

  public long scheduleMicroOps() {
    return scheduleMicroOps.sum();
  }

  public long scheduleMicroLatencyNanos() {
    return scheduleMicroLatencyNanos.sum();
  }

  public double scheduleMicroAverageMicros() {
    return averageMicros(scheduleMicroLatencyNanos.sum(), scheduleMicroOps.sum());
  }

  public long offerMicroOps() {
    return offerMicroOps.sum();
  }

  public long offerMicroLatencyNanos() {
    return offerMicroLatencyNanos.sum();
  }

  public double offerMicroAverageMicros() {
    return averageMicros(offerMicroLatencyNanos.sum(), offerMicroOps.sum());
  }

  public long pollMicroOps() {
    return pollMicroOps.sum();
  }

  public long pollMicroLatencyNanos() {
    return pollMicroLatencyNanos.sum();
  }

  public double pollMicroAverageMicros() {
    return averageMicros(pollMicroLatencyNanos.sum(), pollMicroOps.sum());
  }

  public long drainTickOps() {
    return drainTickOps.sum();
  }

  public long drainTickLatencyNanos() {
    return drainTickLatencyNanos.sum();
  }

  public double drainTickAverageMicros() {
    return averageMicros(drainTickLatencyNanos.sum(), drainTickOps.sum());
  }

  public long cancelOps() {
    return cancelOps.sum();
  }

  public long cancelLatencyNanos() {
    return cancelLatencyNanos.sum();
  }

  public double cancelAverageMicros() {
    return averageMicros(cancelLatencyNanos.sum(), cancelOps.sum());
  }

  private static double averageMicros(long nanos, long ops) {
    if (ops == 0L) {
      return 0.0d;
    }
    return nanos / (double) ops / 1_000.0d;
  }
}
