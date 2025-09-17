package com.mojang.brigadier.builder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;

public final class RequiredArgumentBuilder<S, T>
    extends ArgumentBuilder<S, RequiredArgumentBuilder<S, T>> {
  private final String name;
  private final ArgumentType<T> type;

  private RequiredArgumentBuilder(String name, ArgumentType<T> type) {
    this.name = name;
    this.type = type;
  }

  public static <S, T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
    return new RequiredArgumentBuilder<>(name, type);
  }

  public String getName() {
    return name;
  }

  public ArgumentType<T> getType() {
    return type;
  }

  @Override
  protected RequiredArgumentBuilder<S, T> getThis() {
    return this;
  }

  @Override
  public CommandDispatcher.CommandNode<S> build() {
    CommandDispatcher.ArgumentCommandNode<S, T> node =
        new CommandDispatcher.ArgumentCommandNode<>(name, type, getCommand());
    for (ArgumentBuilder<S, ?> child : getChildren()) {
      node.addChild(child.build());
    }
    return node;
  }
}
