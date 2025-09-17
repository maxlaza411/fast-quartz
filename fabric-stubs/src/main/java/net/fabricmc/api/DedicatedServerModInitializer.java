package net.fabricmc.api;

/** Minimal stub of the Fabric {@code DedicatedServerModInitializer} interface. */
@FunctionalInterface
public interface DedicatedServerModInitializer {
  void onInitializeServer();
}
