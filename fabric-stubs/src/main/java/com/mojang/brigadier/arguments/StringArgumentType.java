package com.mojang.brigadier.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public final class StringArgumentType implements ArgumentType<String> {
  private enum StringType {
    SINGLE,
    GREEDY
  }

  private final StringType type;

  private StringArgumentType(StringType type) {
    this.type = type;
  }

  public static StringArgumentType string() {
    return new StringArgumentType(StringType.SINGLE);
  }

  public static StringArgumentType greedyString() {
    return new StringArgumentType(StringType.GREEDY);
  }

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    if (type == StringType.GREEDY) {
      String remaining = reader.getRemaining();
      reader.setCursor(reader.getTotalLength());
      return remaining.trim();
    }
    return reader.readString();
  }
}
