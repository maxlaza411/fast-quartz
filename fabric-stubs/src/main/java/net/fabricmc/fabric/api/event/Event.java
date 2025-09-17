package net.fabricmc.fabric.api.event;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Extremely small event container used by the stubs to model Fabric's event bus. */
public final class Event<T> {
  private final List<T> listeners = new CopyOnWriteArrayList<>();

  public void register(T listener) {
    listeners.add(listener);
  }

  public void fire(Consumer<T> invoker) {
    for (T listener : listeners) {
      invoker.accept(listener);
    }
  }

  public List<T> listeners() {
    return Collections.unmodifiableList(listeners);
  }

  public void clearListeners() {
    listeners.clear();
  }
}
