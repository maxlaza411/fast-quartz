package net.fabricmc.fabric.api.command.v2;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;

/** Stubbed copy of Fabric's command registration callback. */
public interface CommandRegistrationCallback {
  Event<CommandRegistrationCallback> EVENT = new Event<>();

  void register(
      CommandDispatcher<ServerCommandSource> dispatcher,
      CommandRegistryAccess registryAccess,
      RegistrationEnvironment environment);
}
