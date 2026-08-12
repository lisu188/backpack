# Backpack Optimization Experiment

A small **Kotlin + JavaFX** experiment exploring a mutation-based heuristic for the knapsack/backpack optimization problem.

The application generates a set of random item weights, represents a candidate selection as a Boolean genome, mutates one position at a time and keeps improvements that remain within the configured capacity. Progress is displayed in a simple JavaFX UI while the optimization runs on a coroutine dispatcher.

## What it demonstrates

- Kotlin/JVM application structure with Gradle Kotlin DSL
- simple stochastic/local-search optimization
- immutable-vs-mutated candidate handling
- JavaFX UI wiring
- coroutine-based separation of background computation and UI updates

## Run

```bash
./gradlew run
```

The UI lets you choose the backpack capacity and number of generated items before starting the search.

## Status

**Legacy learning experiment (2022–2023).** This is intentionally a compact demonstration rather than a production optimizer. It uses older Kotlin/JavaFX dependencies and deliberately simple search logic; current production-style engineering is better represented by repositories such as `winrisk`, `fall-of-nouraajd` and `clash-disassembly`.
