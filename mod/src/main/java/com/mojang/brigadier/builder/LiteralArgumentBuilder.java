package com.mojang.brigadier.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Minimal stub of Brigadier's {@code LiteralArgumentBuilder}. */
public final class LiteralArgumentBuilder<S> {
  private final String literal;
  private final Map<String, LiteralArgumentBuilder<S>> children = new LinkedHashMap<>();
  private Predicate<S> requirement = source -> true;
  private Command<S> command;

  public LiteralArgumentBuilder(String literal) {
    this.literal = Objects.requireNonNull(literal, "literal");
  }

  public LiteralArgumentBuilder<S> requires(Predicate<S> requirement) {
    this.requirement = Objects.requireNonNull(requirement, "requirement");
    return this;
  }

  public LiteralArgumentBuilder<S> then(LiteralArgumentBuilder<S> child) {
    Objects.requireNonNull(child, "child");
    children.put(child.literal, child);
    return this;
  }

  public LiteralArgumentBuilder<S> executes(Command<S> command) {
    this.command = Objects.requireNonNull(command, "command");
    return this;
  }

  public String getLiteral() {
    return literal;
  }

  public LiteralCommandNode<S> build() {
    Map<String, LiteralCommandNode<S>> builtChildren = new LinkedHashMap<>();
    for (LiteralArgumentBuilder<S> child : children.values()) {
      builtChildren.put(child.literal, child.build());
    }
    return new LiteralCommandNode<>(literal, requirement, command, builtChildren);
  }
}
