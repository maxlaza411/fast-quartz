package com.mojang.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Minimal stub of Brigadier's {@code CommandDispatcher}. */
public final class CommandDispatcher<S> {
  private final Map<String, LiteralCommandNode<S>> literals = new LinkedHashMap<>();

  public void register(LiteralArgumentBuilder<S> builder) {
    Objects.requireNonNull(builder, "builder");
    LiteralCommandNode<S> node = builder.build();
    literals.put(node.getLiteral(), node);
  }

  public Collection<LiteralCommandNode<S>> getLiterals() {
    return Collections.unmodifiableCollection(literals.values());
  }

  public int execute(String command, S source) {
    LiteralCommandNode<S> node = literals.get(command);
    if (node == null || node.getCommand() == null || !node.getRequirement().test(source)) {
      return 0;
    }
    return node.getCommand().run(new CommandContext<>(source));
  }
}
