package com.mojang.brigadier.tree;

import com.mojang.brigadier.Command;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Minimal stub of Brigadier's {@code LiteralCommandNode}. */
public final class LiteralCommandNode<S> {
  private final String literal;
  private final Predicate<S> requirement;
  private final Command<S> command;
  private final Map<String, LiteralCommandNode<S>> children;

  public LiteralCommandNode(
      String literal,
      Predicate<S> requirement,
      Command<S> command,
      Map<String, LiteralCommandNode<S>> children) {
    this.literal = Objects.requireNonNull(literal, "literal");
    this.requirement = requirement != null ? requirement : source -> true;
    this.command = command;
    this.children = new LinkedHashMap<>(children);
  }

  public String getLiteral() {
    return literal;
  }

  public Predicate<S> getRequirement() {
    return requirement;
  }

  public Command<S> getCommand() {
    return command;
  }

  public Collection<LiteralCommandNode<S>> getChildren() {
    return Collections.unmodifiableCollection(children.values());
  }

  public LiteralCommandNode<S> getChild(String name) {
    return children.get(name);
  }
}
