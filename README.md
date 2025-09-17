# fast-quartz

Fast Quartz's redstone simulation stack for Minecraft Java 1.20.1.

## Modules

- `fast-quartz-core`: Fabric mod that boots the Fast Quartz simulation engine (currently scaffolded).
- `headless-runner`: Dedicated-server wrapper mod used for headless execution flows. Depends on `fast-quartz-core`.

## Requirements

- Java 17
- Gradle (wrapper provided)

## Common tasks

- `./gradlew check` – runs code style (Spotless), Error Prone, and unit tests.
- `./gradlew build` – produces remapped mod jars for each module.
- `./gradlew :headless-runner:run` – primes the headless wrapper; place a vanilla 1.20.1 server jar under `headless-runner/run/headless/runtime/` to launch Minecraft.

## Locked interfaces

See [`docs/LOCKS.md`](docs/LOCKS.md) for the frozen technical specification and integration contracts.
