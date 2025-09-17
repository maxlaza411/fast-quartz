package com.mojang.brigadier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** Minimal string reader used to parse command inputs for the stub Brigadier implementation. */
public final class StringReader {
  private final String string;
  private int cursor;

  public StringReader(String string) {
    this.string = string;
  }

  public boolean canRead() {
    return cursor < string.length();
  }

  public char peek() {
    return string.charAt(cursor);
  }

  public char read() {
    return string.charAt(cursor++);
  }

  public void skipWhitespace() {
    while (canRead() && Character.isWhitespace(peek())) {
      cursor++;
    }
  }

  public int getCursor() {
    return cursor;
  }

  public void setCursor(int cursor) {
    this.cursor = cursor;
  }

  public int getTotalLength() {
    return string.length();
  }

  public String getString() {
    return string;
  }

  public String getRemaining() {
    return string.substring(cursor);
  }

  public String readUnquotedString() {
    int start = cursor;
    while (canRead() && !Character.isWhitespace(peek())) {
      cursor++;
    }
    return string.substring(start, cursor);
  }

  public String readString() throws CommandSyntaxException {
    if (!canRead()) {
      throw new CommandSyntaxException("Expected string");
    }
    char next = peek();
    if (next == '\"' || next == '\'') {
      cursor++;
      StringBuilder result = new StringBuilder();
      while (canRead()) {
        char c = read();
        if (c == next) {
          return result.toString();
        }
        if (c == '\\' && canRead()) {
          result.append(read());
        } else {
          result.append(c);
        }
      }
      throw new CommandSyntaxException("Unclosed quoted string");
    }
    return readUnquotedString();
  }
}
