package dev.fastquartz.faststone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FaststoneModTest {
  @Test
  void modIdIsStable() {
    assertEquals("faststone", FaststoneMod.MOD_ID);
  }
}
