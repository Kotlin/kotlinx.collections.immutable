# Test plan

The 575 missed lines partition into 8 areas along production-file boundaries, each owned by one new test file (or two where the area spans unrelated public surfaces). One writer per area, exclusive file ownership, no edits to existing tests. Every missed line is claimed by exactly one area (the packets also carried two phantom records, see [04-coverage-map.md](04-coverage-map.md)).

| area | missed lines | new test files (under `core/commonTest/src/`) |
|---|---|---|
| builder add/insert/addAll paths | 116 | `contract/list/PersistentListBuilderInsertionTest.kt` |
| builder get/set/remove paths | 79 | `contract/list/PersistentListBuilderRemovalTest.kt` |
| immutable PersistentVector ops | 138 | `contract/list/PersistentVectorTest.kt` |
| list iterators | 34 | `contract/list/PersistentListIteratorTest.kt` |
| hash map trie + entry iterators | 90 | `contract/map/PersistentHashMapExtraTest.kt`, `contract/map/PersistentMapEntryIteratorTest.kt` |
| extensions + interface defaults | 58 | `contract/ConversionsTest.kt`, `contract/DeprecatedApiTest.kt` |
| hash set trie + ordered set | 34 | `contract/set/PersistentHashSetExtraTest.kt`, `contract/set/PersistentOrderedSetExtraTest.kt` |
| adapters + ListImplementation | 28 | `contract/AdaptersTest.kt`, `implementations/InternalUtilitiesTest.kt` |

Rules for the new tests:

- Public-API scenarios first; internal-API white-box only where the public API cannot reach a line, stated per case in [04-coverage-map.md](04-coverage-map.md).
- Deterministic and platform-independent: no unseeded `Random`, collisions shaped with the existing `tests.ObjectWrapper`/`IntWrapper` helpers, no reliance on `String.hashCode` or hash iteration order. `commonTest` runs on JVM, JS, Wasm and Native.
- One scenario should sweep many related lines (a single large-list insertion covers a whole recursion); near-duplicate variants are rejected in review.
- Assertions compare against a plain stdlib reference collection performing the same operations, following the existing contract-test style.

Useful size boundaries for list tests: 32 (tail limit), 33 (first root), 1056/1057 (first height growth), and back down. Map/set tries branch on 5-bit hash segments; two `IntWrapper`s with equal hashCode collide fully after 6 levels.
