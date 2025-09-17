package dev.fastquartz.fastquartz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FastQuartzModTest {
  @Test
  void modIdIsStable() {
    assertEquals("fastquartz", FastQuartzMod.MOD_ID);
  }
}
