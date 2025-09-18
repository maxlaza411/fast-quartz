package net.minecraft.text;

import java.util.Objects;

/** Minimal stub of Minecraft's {@code Text}. */
public final class Text {
  private final String content;

  private Text(String content) {
    this.content = Objects.requireNonNull(content, "content");
  }

  public static Text literal(String content) {
    return new Text(content);
  }

  public String getString() {
    return content;
  }
}
