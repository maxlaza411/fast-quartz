package dev.fastquartz.fastquartz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FastQuartzModCommandsTest {
  private static final CommandRegistryAccess DUMMY_REGISTRY_ACCESS = new CommandRegistryAccess() {};
  private static final ServerCommandSource SOURCE = new ServerCommandSource();

  private final CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();
  private final FastQuartzMod mod = new FastQuartzMod();

  @BeforeEach
  void setUp() {
    CommandRegistrationCallback.EVENT.clearListeners();
    mod.onInitialize();
    CommandRegistrationCallback.EVENT.fire(
        callback ->
            callback.register(dispatcher, DUMMY_REGISTRY_ACCESS, RegistrationEnvironment.ALL));
  }

  @Test
  void registersRunCommand() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq run", SOURCE));
    assertEquals(
        Command.SINGLE_SUCCESS, dispatcher.execute("fq run --tps 200 --until done", SOURCE));
  }

  @Test
  void registersStepCommand() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq step", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq step 4", SOURCE));
  }

  @Test
  void registersLifecycleCommands() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq halt", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq reset", SOURCE));
  }

  @Test
  void registersSnapshotCommands() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq snapshot", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq snapshot save checkpoint", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq snapshot load checkpoint", SOURCE));
  }

  @Test
  void registersTraceCommands() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq trace", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq trace add trace:alu", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq trace rm trace:alu", SOURCE));
    assertEquals(
        Command.SINGLE_SUCCESS, dispatcher.execute("fq trace export vcd dump.vcd", SOURCE));
  }

  @Test
  void registersWatchCommands() throws CommandSyntaxException {
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq watch", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq watch add score.foo", SOURCE));
    assertEquals(
        Command.SINGLE_SUCCESS, dispatcher.execute("fq watch add score.foo break", SOURCE));
    assertEquals(Command.SINGLE_SUCCESS, dispatcher.execute("fq watch rm score.foo", SOURCE));
  }
}
