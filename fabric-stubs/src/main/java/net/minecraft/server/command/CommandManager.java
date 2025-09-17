package net.minecraft.server.command;

/** Minimal subset of Minecraft's {@code CommandManager} used by tests and stubs. */
public final class CommandManager {
  private CommandManager() {}

  public enum RegistrationEnvironment {
    DEDICATED,
    INTEGRATED,
    ALL
  }
}
