# Backpack Optimization Experiment

A small **Kotlin + JavaFX** experiment exploring a mutation-based heuristic for the knapsack/backpack optimization problem.

The application generates a set of random item weights, represents a candidate selection as a Boolean genome, mutates one position at a time and keeps improvements that remain within the configured capacity. Progress is displayed in a simple JavaFX UI while the optimization runs on a coroutine dispatcher.

## What it demonstrates

- Kotlin/JVM application structure with Gradle Kotlin DSL
- simple stochastic/local-search optimization
- constant-time single-bit candidate evaluation
- JavaFX UI wiring
- coroutine-based separation of background computation and UI updates

## Run

```bash
./gradlew run
```

The UI accepts whole-number capacity and item count from 1 to 100,000 (defaults 10 and 10,000). Start owns one background search; use Cancel to stop it before starting another. Closing the application cancels its search. Progress is reported in batches, and all control reads and writes stay on the JavaFX thread.

The heuristic still tries 9,999,999 random single-bit changes and accepts only strict improvements within capacity; it does not guarantee a globally optimal solution. Candidate evaluation maintains the selected weight instead of copying and rescoring the whole genome.

## Tests

```bash
./gradlew test
```

Read-only CI compiles the application and runs deterministic tests with Java 17 and the checked-in Gradle wrapper. Tests cover input bounds, agreement with independent full rescoring, reproducible injected randomness, and cancellation during generation and a hot loop without improvements. They do not launch or visually validate JavaFX. The existing Kotlin, JavaFX, coroutine and Gradle versions are retained.

## Status

**Legacy learning experiment (2022–2023).** This is intentionally a compact demonstration rather than a production optimizer. It uses older Kotlin/JavaFX dependencies and deliberately simple search logic; current production-style engineering is better represented by repositories such as `winrisk`, `fall-of-nouraajd` and `clash-disassembly`.
