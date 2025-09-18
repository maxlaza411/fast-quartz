package net.minecraft.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/** Minimal stub of Minecraft's {@code CommandManager}. */
public final class CommandManager {
  private CommandManager() {}

  public static LiteralArgumentBuilder<ServerCommandSource> literal(String literal) {
    return new LiteralArgumentBuilder<>(literal);
  }

  /** Mirrors Fabric's registration environment enumeration. */
  public enum RegistrationEnvironment {
    DEDICATED,
    INTEGRATED
  }
}
