package dev.fastquartz.headless;

import dev.fastquartz.faststone.FaststoneMod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper that would normally launch the dedicated server. For now it only validates the
 * runtime layout and records the command that should be executed when the vanilla server jar is
 * available.
 */
public final class HeadlessServerLauncher {
  private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessServerLauncher.class);
  private static final String SERVER_JAR = "runtime/minecraft-server-1.20.1.jar";

  private HeadlessServerLauncher() {}

  public static void main(String[] args) throws Exception {
    LOGGER.info("Faststone headless wrapper starting (mod id: {}).", FaststoneMod.MOD_ID);

    Path runDir = Path.of("run/headless");
    Files.createDirectories(runDir);
    LOGGER.info("Run directory: {}", runDir.toAbsolutePath());

    Path serverJar = runDir.resolve(SERVER_JAR);
    if (!Files.exists(serverJar)) {
      LOGGER.warn(
          "Server jar {} is missing. Place the vanilla 1.20.1 dedicated server jar at this location"
              + " to enable execution.",
          serverJar);
      LOGGER.warn("Wrapper is exiting without launching the server due to the missing jar.");
      return;
    }

    List<String> command =
        Arrays.asList(
            System.getProperty("java.home") + "/bin/java",
            "-Xmx2G",
            "-Xms2G",
            "-jar",
            serverJar.toString(),
            "nogui");

    LOGGER.info("Launching server: {}", command);
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(runDir.toFile());
    builder.inheritIO();
    Process process = builder.start();
    int exitCode = process.waitFor();
    LOGGER.info("Server process finished with exit code {}.", exitCode);
  }
}
