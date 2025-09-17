package com.mojang.brigadier.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Simplified command context capturing the input string, parsed arguments, and source object. */
public final class CommandContext<S> {
  private final S source;
  private final String input;
  private final Map<String, Object> arguments;

  public CommandContext(S source, String input, Map<String, Object> arguments) {
    this.source = source;
    this.input = input;
    this.arguments = new LinkedHashMap<>(arguments);
  }

  public S getSource() {
    return source;
  }

  public String getInput() {
    return input;
  }

  public Map<String, Object> getArguments() {
    return Collections.unmodifiableMap(arguments);
  }
}
