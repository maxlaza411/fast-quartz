package net.fabricmc.api;

/**
 * Minimal stub of Fabric's {@code ModInitializer}. The production build will depend on the actual
 * Fabric API, but for the engine scaffolding we only need to model the entrypoint contract.
 */
public interface ModInitializer {
  void onInitialize();
}
