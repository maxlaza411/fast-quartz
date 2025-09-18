package dev.fastquartz.mod;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.fastquartz.engine.FastQuartzEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FastQuartzMod implements ModInitializer {
  public static final String MOD_ID = "fast_quartz";
  private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  private static final String[] SUBCOMMANDS = {
    "run", "step", "halt", "reset", "snapshot", "trace", "watch"
  };

  private FastQuartzEngine engine;

  @Override
  public void onInitialize() {
    engine = FastQuartzEngine.create(20);
    LOGGER.info("Fast Quartz mod initialized with target TPS {}", engine.targetTps());

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
  }

  private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
    LiteralArgumentBuilder<ServerCommandSource> root =
        CommandManager.literal("fs").requires(source -> source.hasPermissionLevel(2));

    for (String subcommand : SUBCOMMANDS) {
      root.then(
          CommandManager.literal(subcommand)
              .executes(context -> handleCommand(context.getSource(), subcommand)));
    }

    dispatcher.register(root);
  }

  private int handleCommand(ServerCommandSource source, String action) {
    LOGGER.info("/fs {} invoked by {}", action, source.getName());
    source.sendFeedback(
        () -> Text.literal("Fast Quartz command '" + action + "' executed (stub)."), false);
    return Command.SINGLE_SUCCESS;
  }
}
