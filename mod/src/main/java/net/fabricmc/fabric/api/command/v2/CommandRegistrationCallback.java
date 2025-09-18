package net.fabricmc.fabric.api.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/** Minimal stub of Fabric's {@code CommandRegistrationCallback}. */
public interface CommandRegistrationCallback {
  Event<CommandRegistrationCallback> EVENT =
      EventFactory.createArrayBacked(
          CommandRegistrationCallback.class,
          listeners ->
              (dispatcher, registryAccess, environment) -> {
                for (CommandRegistrationCallback callback : listeners) {
                  callback.register(dispatcher, registryAccess, environment);
                }
              });

  void register(
      CommandDispatcher<ServerCommandSource> dispatcher,
      CommandRegistryAccess registryAccess,
      CommandManager.RegistrationEnvironment environment);

  /** Placeholder for the registry access interface. */
  interface CommandRegistryAccess {
    // Marker interface for stubbed registry access.
  }
}
