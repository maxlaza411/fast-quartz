package dev.fastquartz.faststone;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Root mod initializer. Currently wires logging and ensures the mod loads under Fabric. */
public final class FaststoneMod implements ModInitializer {
  public static final String MOD_ID = "faststone";
  private static final Logger LOGGER = LoggerFactory.getLogger(FaststoneMod.class);

  @Override
  public void onInitialize() {
    LOGGER.info("Faststone core mod scaffolding loaded.");
  }
}
