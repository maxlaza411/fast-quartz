package dev.fastquartz.headless;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides a dedicated-server entrypoint that wires the headless runner wrapper. */
public final class HeadlessEntrypoint implements DedicatedServerModInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessEntrypoint.class);

  @Override
  public void onInitializeServer() {
    LOGGER.info("Fast Quartz headless runner initialized.");
  }
}
