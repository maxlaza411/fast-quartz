package com.mojang.brigadier.builder;

import com.mojang.brigadier.CommandDispatcher;

public final class LiteralArgumentBuilder<S> extends ArgumentBuilder<S, LiteralArgumentBuilder<S>> {
  private final String literal;

  private LiteralArgumentBuilder(String literal) {
    this.literal = literal;
  }

  public static <S> LiteralArgumentBuilder<S> literal(String name) {
    return new LiteralArgumentBuilder<>(name);
  }

  public String getLiteral() {
    return literal;
  }

  @Override
  protected LiteralArgumentBuilder<S> getThis() {
    return this;
  }

  @Override
  public CommandDispatcher.CommandNode<S> build() {
    CommandDispatcher.LiteralCommandNode<S> node =
        new CommandDispatcher.LiteralCommandNode<>(literal, getCommand());
    for (ArgumentBuilder<S, ?> child : getChildren()) {
      node.addChild(child.build());
    }
    return node;
  }
}
