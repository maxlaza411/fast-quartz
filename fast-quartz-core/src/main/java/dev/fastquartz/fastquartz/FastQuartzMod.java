package dev.fastquartz.fastquartz;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint for the Fast Quartz core mod. Currently just wires no-op command stubs. */
public final class FastQuartzMod implements ModInitializer {
  public static final String MOD_ID = "fastquartz";
  private static final Logger LOGGER = LoggerFactory.getLogger(FastQuartzMod.class);

  @Override
  public void onInitialize() {
    LOGGER.info("Fast Quartz core initializing (command scaffolding active)");
    CommandRegistrationCallback.EVENT.register(this::registerCommands);
  }

  private void registerCommands(
      CommandDispatcher<ServerCommandSource> dispatcher,
      CommandRegistryAccess registryAccess,
      RegistrationEnvironment environment) {
    dispatcher.register(buildRootCommand());
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildRootCommand() {
    LiteralArgumentBuilder<ServerCommandSource> root =
        literal("fq").executes(FastQuartzMod::logInvocation);

    root.then(buildRunCommand());
    root.then(buildStepCommand());
    root.then(literal("halt").executes(FastQuartzMod::logInvocation));
    root.then(literal("reset").executes(FastQuartzMod::logInvocation));
    root.then(buildSnapshotCommand());
    root.then(buildTraceCommand());
    root.then(buildWatchCommand());

    return root;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildRunCommand() {
    LiteralArgumentBuilder<ServerCommandSource> run =
        literal("run").executes(FastQuartzMod::logInvocation);
    run.then(greedyStringArgument("options").executes(FastQuartzMod::logInvocation));
    return run;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildStepCommand() {
    LiteralArgumentBuilder<ServerCommandSource> step =
        literal("step").executes(FastQuartzMod::logInvocation);
    step.then(integerArgument("ticks").executes(FastQuartzMod::logInvocation));
    return step;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildSnapshotCommand() {
    LiteralArgumentBuilder<ServerCommandSource> snapshot =
        literal("snapshot").executes(FastQuartzMod::logInvocation);
    snapshot.then(
        literal("save").then(stringArgument("name").executes(FastQuartzMod::logInvocation)));
    snapshot.then(
        literal("load").then(stringArgument("name").executes(FastQuartzMod::logInvocation)));
    return snapshot;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildTraceCommand() {
    LiteralArgumentBuilder<ServerCommandSource> trace =
        literal("trace").executes(FastQuartzMod::logInvocation);
    trace.then(
        literal("add").then(stringArgument("target").executes(FastQuartzMod::logInvocation)));
    trace.then(literal("rm").then(stringArgument("target").executes(FastQuartzMod::logInvocation)));
    trace.then(
        literal("export")
            .then(
                literal("vcd")
                    .then(stringArgument("file").executes(FastQuartzMod::logInvocation))));
    return trace;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildWatchCommand() {
    LiteralArgumentBuilder<ServerCommandSource> watch =
        literal("watch").executes(FastQuartzMod::logInvocation);
    watch.then(buildWatchAddCommand());
    watch.then(
        literal("rm").then(stringArgument("expression").executes(FastQuartzMod::logInvocation)));
    return watch;
  }

  private LiteralArgumentBuilder<ServerCommandSource> buildWatchAddCommand() {
    LiteralArgumentBuilder<ServerCommandSource> add = literal("add");
    RequiredArgumentBuilder<ServerCommandSource, String> expr =
        stringArgument("expression").executes(FastQuartzMod::logInvocation);
    expr.then(literal("break").executes(FastQuartzMod::logInvocation));
    add.then(expr);
    return add;
  }

  private static LiteralArgumentBuilder<ServerCommandSource> literal(String literal) {
    return LiteralArgumentBuilder.literal(literal);
  }

  private static RequiredArgumentBuilder<ServerCommandSource, Integer> integerArgument(
      String name) {
    return RequiredArgumentBuilder.argument(name, IntegerArgumentType.integer(1));
  }

  private static RequiredArgumentBuilder<ServerCommandSource, String> greedyStringArgument(
      String name) {
    return RequiredArgumentBuilder.argument(name, StringArgumentType.greedyString());
  }

  private static RequiredArgumentBuilder<ServerCommandSource, String> stringArgument(String name) {
    return RequiredArgumentBuilder.argument(name, StringArgumentType.string());
  }

  private static int logInvocation(CommandContext<ServerCommandSource> context) {
    LOGGER.info("Invoked /{}", context.getInput());
    return Command.SINGLE_SUCCESS;
  }
}
