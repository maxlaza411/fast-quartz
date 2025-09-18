package dev.fastquartz.engine.event;

/** Locked event ordinals for deterministic scheduling. */
public enum EventType {
  SCHEDULED(0),
  LOCAL_UPDATE(1),
  POWER_PROP(2),
  STATEFUL(3),
  PISTON_TXN(4),
  OBSERVER_EMIT(5),
  XREG_OUT(6),
  XREG_IN(7);

  private final int eventOrdinal;

  EventType(int eventOrdinal) {
    this.eventOrdinal = eventOrdinal;
  }

  public int eventOrdinal() {
    return eventOrdinal;
  }
}
