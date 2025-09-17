package com.mojang.brigadier.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public final class IntegerArgumentType implements ArgumentType<Integer> {
  private final int minimum;

  private IntegerArgumentType(int minimum) {
    this.minimum = minimum;
  }

  public static IntegerArgumentType integer(int minimum) {
    return new IntegerArgumentType(minimum);
  }

  @Override
  public Integer parse(StringReader reader) throws CommandSyntaxException {
    String value = reader.readUnquotedString();
    if (value.isEmpty()) {
      throw new CommandSyntaxException("Expected integer");
    }
    try {
      int parsed = Integer.parseInt(value);
      if (parsed < minimum) {
        throw new CommandSyntaxException("Integer must be at least " + minimum);
      }
      return parsed;
    } catch (NumberFormatException ex) {
      throw new CommandSyntaxException("Invalid integer: " + value);
    }
  }
}
