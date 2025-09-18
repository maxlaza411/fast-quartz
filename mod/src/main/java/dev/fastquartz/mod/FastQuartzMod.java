package dev.fastquartz.mod;

import dev.fastquartz.engine.FastQuartzEngine;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FastQuartzMod implements ModInitializer {
  public static final String MOD_ID = "fast_quartz";
  private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    FastQuartzEngine engine = FastQuartzEngine.create(20);
    LOGGER.info("Fast Quartz mod initialized with target TPS {}", engine.targetTps());
  }
}
