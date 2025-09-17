# Fast Quartz Lockfile

This document freezes the interfaces and invariants that require explicit approval before changes. Revisions to any entry must include a migration note and a version bump of the affected artifact.

## 1. Version matrix
- **Minecraft:** 1.20.1 (Java 17 runtime)
- **Fabric Loader:** 0.15.11
- **Fabric API:** 0.91.0+1.20.1
- **Yarn mappings:** 1.20.1+build.10 (v2)
- **Java toolchain:** 17
- **Gradle:** 8.6 (via wrapper)

## 2. Mixin injection points
- `net.minecraft.server.world.ServerWorld#tick(BooleanSupplier)` – redirect scheduled tick processing to the Fast Quartz engine.
- `net.minecraft.server.world.ServerTickScheduler` family (`schedule`, `tick`, `runNextTicks`) – intercept and route redstone-relevant scheduled ticks into the engine.
- Block neighbour change hooks (e.g. `Block#neighborUpdate`, `Block#scheduledTick`) – wrap vanilla recursion to enqueue deterministic engine events.
- `PistonBlock#onSyncedBlockEvent` – shortcut into piston transaction handling.

## 3. Event key schema
- Key: `(gameTick, microPhase, regionId, localOrder)`
- `microPhase` enumerates P0–P9 as defined in the engine specification.
- `regionId = (chunkX >> R, chunkZ >> R)` with `R = 2` (regions are 4×4 chunks).
- `localOrder = BlockPosYZX ⊕ eventTypeOrdinal`, i.e. `(y << 8) | (z << 4) | x` combined with a deterministic event ordinal.

## 4. Region size and threading policy
- Default region span: 4×4 chunks (64×64 blocks), tunable but locked for determinism baselines.
- One worker per active region, synchronised through micro-phase barriers between P2–P8 loops.
- Cross-region mailboxes flushed during P7 with `(srcRegion, dstRegion, tick, micro, seq)` ordering.

## 5. ShadowWorld API
- Read-only passthrough for blocks untouched in the current tick.
- Buffered writes per chunk-section (16×16×16) with commit in P9.
- API surface: `getBlockState`, `setBlockState`, `scheduleTick`, `markNeighborChanged`, `getBlockEntity`, `readContainerSignal`.
- Guarantees: no lighting/physics side effects, atomic visibility at commit.

## 6. Netlist representation
- Node table: contiguous array keyed by node id with `(type, BlockPos, stateBits, powerIn, powerOut)`.
- Edge table: bit-packed adjacency for dust plus directed component edges.
- Island index: union–find with BFS invalidation when block topology changes.

## 7. Memory encoders
- `ComparatorLinear(nBits, radix=15)`
- `RepeaterDelayLFSR`
- `ContainerROM` flavours: barrel, lectern
- Encoders are deterministic, versioned via configuration manifest.

## 8. Trace labelling
- Labels embed in sign/block-entity NBT: prefix `trace:` followed by a colon-delimited hierarchy (e.g. `trace:IF:stage0`).
- Scope inherits along redstone graph connections; duplicates resolve by lexical order.

## 9. Replay format
- Versioned binary stream: header contains semantic version + engine build hash.
- Events encoded with LEB128 lengths; payload: `(eventKey, payloadType, payloadBytes)`.
- Append-only; includes run metadata (seed, command line, fidelity mode).

## 10. Stop-condition DSL
- Boolean expressions referencing scoreboard values and watch expressions (`==`, `!=`, `<`, `<=`, `>`, `>=`).
- Conjunction (`&&`), disjunction (`||`), and parentheses permitted; no user functions.
- Resolved against deterministic snapshots to preserve replayability.
