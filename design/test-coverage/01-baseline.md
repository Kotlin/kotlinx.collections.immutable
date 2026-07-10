# Baseline

Kover 0.9.8, wired the same way as the other plugin versions (`koverVersion` in `gradle.properties`, version in `settings.gradle.kts` pluginManagement, bare id in `core/build.gradle.kts`). The report task runs `jvmTest` and writes `core/build/reports/kover/report.xml` (JaCoCo schema; a line is missed iff its `ci` counter is 0 and `mi` is positive, see the measurement note in [04-coverage-map.md](04-coverage-map.md)).

Two baselines on master (ad741d1), before any new test:

| suite | line coverage | missed lines |
|---|---|---|
| deterministic (`-PjvmTestExcludes=tests.stress.*`) | 79.61% (2245/2820) | 575 in 26 files |
| full, including stress | 95.11% (2682/2820) | 140 in 22 files |

The 437-line difference is coverage that exists only because the unseeded stress tests happened to roll the right sizes and hash codes on that run. It concentrates in the large-list trie machinery: `PersistentVectorBuilder.kt` (185 of its 188 missed lines), `PersistentVector.kt` (117 of 118), and the collision paths of both `TrieNode.kt` files. Outside of stress, the deterministic suite essentially never built lists past 32 elements (the Guava testlib suites use collections of sizes 0, 1 and 3), which left the trie machinery to stress-test luck.

The 140 lines missed even with stress had no coverage at all: the whole `adapters` package, 48 lines of `extensions.kt` overloads and deprecated aliases, `ListImplementation.orderedHashCode/orderedEquals`, the `PersistentCollection` interface default bodies, and assorted two-arg `remove(key, value)` collision paths.

Timing on this machine: deterministic run ~40 s, full run ~3 min. Instrumentation fits in the existing 1024m `jvmTest` heap.
