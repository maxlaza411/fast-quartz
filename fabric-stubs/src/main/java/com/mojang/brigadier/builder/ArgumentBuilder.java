package com.mojang.brigadier.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;

public abstract class ArgumentBuilder<S, T extends ArgumentBuilder<S, T>> {
  private final List<ArgumentBuilder<S, ?>> children = new ArrayList<>();
  private Command<S> command;

  public T then(ArgumentBuilder<S, ?> child) {
    children.add(child);
    return getThis();
  }

  public T executes(Command<S> command) {
    this.command = command;
    return getThis();
  }

  public List<ArgumentBuilder<S, ?>> getChildren() {
    return children;
  }

  public Command<S> getCommand() {
    return command;
  }

  protected abstract T getThis();

  public abstract CommandDispatcher.CommandNode<S> build();
}
