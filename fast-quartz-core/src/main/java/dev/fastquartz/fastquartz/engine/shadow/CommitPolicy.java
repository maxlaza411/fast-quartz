package dev.fastquartz.fastquartz.engine.shadow;

/** Policy hints for shadow world commits. */
public enum CommitPolicy {
  /** Apply states silently without triggering vanilla neighbour recursion or lighting. */
  SILENT_NO_PHYSICS
}
