package net.minecraft.server.command;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.text.Text;

/** Minimal stub of Minecraft's {@code ServerCommandSource}. */
public final class ServerCommandSource {
  private final String name;
  private final int permissionLevel;
  private final Consumer<Text> feedbackConsumer;
  private Text lastFeedback;

  public ServerCommandSource(String name, int permissionLevel, Consumer<Text> feedbackConsumer) {
    this.name = Objects.requireNonNull(name, "name");
    this.permissionLevel = permissionLevel;
    this.feedbackConsumer = feedbackConsumer;
  }

  public boolean hasPermissionLevel(int level) {
    return permissionLevel >= level;
  }

  public String getName() {
    return name;
  }

  public void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
    Objects.requireNonNull(feedbackSupplier, "feedbackSupplier");
    Text feedback = feedbackSupplier.get();
    lastFeedback = feedback;
    if (feedbackConsumer != null) {
      feedbackConsumer.accept(feedback);
    }
  }

  public Optional<Text> lastFeedback() {
    return Optional.ofNullable(lastFeedback);
  }
}
