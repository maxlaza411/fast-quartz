package com.mojang.brigadier.exceptions;

/** Basic checked exception mirroring Brigadier's command syntax failures. */
public class CommandSyntaxException extends Exception {
  private static final long serialVersionUID = 1L;

  public CommandSyntaxException(String message) {
    super(message);
  }
}
