package dev.fastquartz.fastquartz.engine.scheduler;

/** Enumeration of supported scheduler event types. */
public enum EvType {
  BLOCK_SCHEDULED_TICK,
  NEIGHBOR_CHANGE,
  POWER_PROPAGATION,
  COMPONENT_EVAL,
  PISTON_TXN,
  OBSERVER_PULSE,
  NETLIST_INVALIDATE,
  ADMIN_CONTROL
}
