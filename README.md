# fast-quartz

Fast Quartz's redstone simulation stack for Minecraft Java 1.20.1.

## Modules

- `engine`: Pure-Java implementation of the Fast Quartz simulation engine. This is where the two-layer netlist
  implementation will live.
- `mod`: Fabric mod that packages the engine for use inside Minecraft. Future integration surfaces (headless runner,
  Fabric entrypoints, etc.) will depend on this module. The module currently compiles against a tiny stub of Fabric's
  `ModInitializer` interface so the build remains self-contained while the full integration work is staged.

## Requirements

- Java 17
- Gradle (wrapper provided)

## Common tasks

- `./gradlew check` – runs code style (Spotless + Checkstyle), Error Prone, and unit tests.
- `./gradlew build` – assembles the engine and mod jars with sources.
- `./gradlew :mod:jar` – builds the stubbed Fabric integration jar used for local testing.

## Locked interfaces

See [`docs/LOCKS.md`](docs/LOCKS.md) for the frozen technical specification and integration contracts.
