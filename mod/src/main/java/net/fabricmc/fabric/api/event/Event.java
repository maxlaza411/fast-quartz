package net.fabricmc.fabric.api.event;

/** Minimal stub of Fabric's {@code Event}. */
public interface Event<T> {
  T invoker();

  void register(T listener);
}
