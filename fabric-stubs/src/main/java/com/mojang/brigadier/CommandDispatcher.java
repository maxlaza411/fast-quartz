package com.mojang.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandDispatcher<S> {
  private final RootCommandNode<S> root = new RootCommandNode<>();

  public CommandNode<S> register(LiteralArgumentBuilder<S> command) {
    CommandNode<S> node = command.build();
    root.addChild(node);
    return node;
  }

  public RootCommandNode<S> getRoot() {
    return root;
  }

  public int execute(String input, S source) throws CommandSyntaxException {
    StringReader reader = new StringReader(input);
    ContextBuilder<S> context = new ContextBuilder<>(source, input);
    CommandNode<S> current = root;
    while (true) {
      reader.skipWhitespace();
      if (!reader.canRead()) {
        break;
      }
      boolean matched = false;
      for (CommandNode<S> child : current.getChildren()) {
        int startCursor = reader.getCursor();
        int checkpoint = context.checkpoint();
        boolean success;
        try {
          success = child.parse(reader, context);
        } catch (CommandSyntaxException e) {
          throw e;
        }
        if (success) {
          matched = true;
          current = child;
          break;
        } else {
          reader.setCursor(startCursor);
          context.restore(checkpoint);
        }
      }
      if (!matched) {
        throw new CommandSyntaxException("Unknown or incomplete command: " + input);
      }
    }
    Command<S> command = current.getCommand();
    if (command == null) {
      throw new CommandSyntaxException("Unknown or incomplete command: " + input);
    }
    CommandContext<S> commandContext = context.build();
    return command.run(commandContext);
  }

  public abstract static class CommandNode<S> {
    private final List<CommandNode<S>> children = new ArrayList<>();
    private final Command<S> command;

    protected CommandNode(Command<S> command) {
      this.command = command;
    }

    public void addChild(CommandNode<S> child) {
      children.add(child);
    }

    public List<CommandNode<S>> getChildren() {
      return children;
    }

    public Command<S> getCommand() {
      return command;
    }

    protected abstract boolean parse(StringReader reader, ContextBuilder<S> context)
        throws CommandSyntaxException;
  }

  public static final class LiteralCommandNode<S> extends CommandNode<S> {
    private final String literal;

    public LiteralCommandNode(String literal, Command<S> command) {
      super(command);
      this.literal = literal;
    }

    @Override
    protected boolean parse(StringReader reader, ContextBuilder<S> context) {
      int start = reader.getCursor();
      String token = reader.readUnquotedString();
      if (token.isEmpty()) {
        reader.setCursor(start);
        return false;
      }
      if (!literal.equals(token)) {
        reader.setCursor(start);
        return false;
      }
      return true;
    }
  }

  public static final class ArgumentCommandNode<S, T> extends CommandNode<S> {
    private final String name;
    private final ArgumentType<T> type;

    public ArgumentCommandNode(String name, ArgumentType<T> type, Command<S> command) {
      super(command);
      this.name = name;
      this.type = type;
    }

    @Override
    protected boolean parse(StringReader reader, ContextBuilder<S> context)
        throws CommandSyntaxException {
      int start = reader.getCursor();
      try {
        T value = type.parse(reader);
        context.putArgument(name, value);
        return true;
      } catch (CommandSyntaxException e) {
        reader.setCursor(start);
        throw e;
      }
    }
  }

  public static final class RootCommandNode<S> extends CommandNode<S> {
    private RootCommandNode() {
      super(null);
    }

    @Override
    protected boolean parse(StringReader reader, ContextBuilder<S> context) {
      return false;
    }
  }

  static final class ContextBuilder<S> {
    private final S source;
    private final String input;
    private final Map<String, Object> arguments = new LinkedHashMap<>();
    private final List<String> insertionOrder = new ArrayList<>();

    ContextBuilder(S source, String input) {
      this.source = source;
      this.input = input;
    }

    void putArgument(String name, Object value) {
      arguments.put(name, value);
      insertionOrder.add(name);
    }

    int checkpoint() {
      return insertionOrder.size();
    }

    void restore(int checkpoint) {
      while (insertionOrder.size() > checkpoint) {
        int index = insertionOrder.size() - 1;
        String key = insertionOrder.remove(index);
        arguments.remove(key);
      }
    }

    CommandContext<S> build() {
      return new CommandContext<>(source, input, arguments);
    }
  }
}
