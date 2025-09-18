package dev.fastquartz.engine.dust;

import java.util.Objects;

/** Identifier for a component port that attaches to the dust network. */
public record DustPort(int componentId, int portIndex) {
  public DustPort {
    if (componentId < 0) {
      throw new IllegalArgumentException("componentId must be non-negative");
    }
    if (portIndex < 0) {
      throw new IllegalArgumentException("portIndex must be non-negative");
    }
  }

  @Override
  public String toString() {
    return "DustPort{" + "componentId=" + componentId + ", portIndex=" + portIndex + '}';
  }

  /** Comparator-friendly view that orders ports by component id then port index. */
  static int compare(DustPort a, DustPort b) {
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");
    int cmp = Integer.compare(a.componentId(), b.componentId());
    if (cmp != 0) {
      return cmp;
    }
    return Integer.compare(a.portIndex(), b.portIndex());
  }
}
