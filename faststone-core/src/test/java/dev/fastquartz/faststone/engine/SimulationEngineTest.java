package dev.fastquartz.faststone.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastquartz.faststone.engine.components.ConstantSourceComponent;
import dev.fastquartz.faststone.engine.components.MaxInputComponent;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {
  @Test
  void elapseSolvesNetworkWithinSingleTick() {
    RedstoneNetwork network = new RedstoneNetwork();
    BlockPos source = new BlockPos(0, 0, 0);
    ConstantSourceComponent lever = new ConstantSourceComponent(0);
    network.addComponent(source, lever);

    BlockPos dust = new BlockPos(1, 0, 0);
    MaxInputComponent dustComponent = new MaxInputComponent(0);
    network.addComponent(dust, dustComponent);

    BlockPos lamp = new BlockPos(2, 0, 0);
    MaxInputComponent lampComponent = new MaxInputComponent(0);
    network.addComponent(lamp, lampComponent);

    network.connect(source, dust);
    network.connect(dust, lamp);

    SimulationEngine engine = new SimulationEngine(network);
    engine.elapse(0); // settle baseline
    assertEquals(0, network.powerAt(lamp));

    lever.setOutput(15);
    engine.markDirty(source);

    ElapseResult result = engine.elapse(0);
    assertEquals(0, result.startTick());
    assertEquals(0, result.endTick());
    assertEquals(3, result.changes().size());
    assertTrue(result.changes().stream().anyMatch(change -> change.position().equals(lamp)));
    assertEquals(15, network.powerAt(lamp));
    assertEquals(0, engine.currentTick());
  }

  @Test
  void delayedComponentWaitsForPropagationDelay() {
    RedstoneNetwork network = new RedstoneNetwork();
    BlockPos source = new BlockPos(0, 0, 0);
    ConstantSourceComponent lever = new ConstantSourceComponent(0);
    network.addComponent(source, lever);

    BlockPos repeater = new BlockPos(1, 0, 0);
    MaxInputComponent repeaterComponent = new MaxInputComponent(2);
    network.addComponent(repeater, repeaterComponent);

    BlockPos lamp = new BlockPos(2, 0, 0);
    MaxInputComponent lampComponent = new MaxInputComponent(0);
    network.addComponent(lamp, lampComponent);

    network.connect(source, repeater);
    network.connect(repeater, lamp);

    SimulationEngine engine = new SimulationEngine(network);
    engine.elapse(0);

    lever.setOutput(15);
    engine.markDirty(source);

    ElapseResult first = engine.elapse(1);
    assertEquals(0, first.startTick());
    assertEquals(1, first.endTick());
    assertEquals(15, network.powerAt(source));
    assertEquals(0, network.powerAt(repeater));
    assertTrue(first.changes().stream().anyMatch(change -> change.position().equals(source)));

    ElapseResult second = engine.elapse(1);
    assertEquals(1, second.startTick());
    assertEquals(2, second.endTick());
    assertEquals(15, network.powerAt(repeater));
    assertEquals(15, network.powerAt(lamp));
    Set<BlockPos> changedPositions =
        second.changes().stream().map(StateChange::position).collect(Collectors.toSet());
    assertTrue(changedPositions.contains(repeater));
    assertTrue(changedPositions.contains(lamp));
    assertEquals(2, engine.currentTick());
  }

  @Test
  void elapseRejectsNegativeTicks() {
    RedstoneNetwork network = new RedstoneNetwork();
    BlockPos source = new BlockPos(0, 0, 0);
    network.addComponent(source, new ConstantSourceComponent(0));

    SimulationEngine engine = new SimulationEngine(network);
    assertThrows(IllegalArgumentException.class, () -> engine.elapse(-1));
  }
}
