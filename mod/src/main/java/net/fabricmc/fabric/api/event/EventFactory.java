package net.fabricmc.fabric.api.event;

import java.util.Objects;
import java.util.function.Function;

/** Minimal stub of Fabric's {@code EventFactory}. */
public final class EventFactory {
  private EventFactory() {}

  public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> invokerFactory) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(invokerFactory, "invokerFactory");
    return new ArrayBackedEvent<>(invokerFactory);
  }

  private static final class ArrayBackedEvent<T> implements Event<T> {
    private final Function<T[], T> invokerFactory;
    private T[] listeners;
    private T invoker;

    @SuppressWarnings("unchecked")
    private ArrayBackedEvent(Function<T[], T> invokerFactory) {
      this.invokerFactory = invokerFactory;
      this.listeners = (T[]) new Object[0];
      rebuildInvoker();
    }

    @Override
    public T invoker() {
      return invoker;
    }

    @Override
    public void register(T listener) {
      Objects.requireNonNull(listener, "listener");
      T[] newListeners = java.util.Arrays.copyOf(listeners, listeners.length + 1);
      newListeners[listeners.length] = listener;
      listeners = newListeners;
      rebuildInvoker();
    }

    private void rebuildInvoker() {
      invoker = invokerFactory.apply(listeners.clone());
    }
  }
}
