package net.fabricmc.api;

/**
 * Minimal stub of the Fabric {@code ModInitializer} interface so the project can compile without
 * the Fabric dependencies during bootstrap.
 */
@FunctionalInterface
public interface ModInitializer {
  void onInitialize();
}
