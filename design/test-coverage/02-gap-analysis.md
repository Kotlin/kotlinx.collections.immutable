# Gap analysis

Why 575 lines had no deterministic coverage, grouped by cause. Line-level detail lives in the baseline report; regenerate with `koverHtmlReport -PjvmTestExcludes=tests.stress.*`.

## Large-list trie machinery, 408 lines

`PersistentVectorBuilder.kt` (188), `PersistentVector.kt` (118), `SmallPersistentVector.kt` (10), the list iterators (41), and their helpers. Everything that only runs once a list crosses 32 elements and grows a root trie: tail push with height growth, insert-into-root with element carry, `addAll(index, ...)` buffer splitting, remove with buffer pull and height decrease, `set` in the root, backward iteration across the tail/trie boundary. The deterministic suite simply never built such lists: Guava testlib works on sizes 0/1/3, and the contract tests stay small. Only the stress tests went big, nondeterministically.

The immutable `PersistentVector` paths (118 lines) were even thinner than the builder paths: contract tests that do grow lists do it through builders, so the pure persistent `adding`/`add(index)`/`removeAt`/`set` recursions were exercised almost nowhere.

## Hash-collision and two-arg-remove paths, 90 lines

`immutableMap/TrieNode.kt` (58), `immutableSet/TrieNode.kt` (25), plus small iterator pieces. Three ingredients are rare in the existing tests: full-hash collision nodes built deterministically (existing collision tests are stress-based), `remove(key, value)` / `remove(element)` variants that compare values before removing, and node-canonicalization on removal (single-child collapse). The `putAll` merge short-circuits for reference-equal subtrees also live here.

## Never-called public API, 103 lines

- `adapters/ReadOnlyCollectionAdapters.kt`, 17 lines: the whole package. No factory creates adapters and no test instantiated one, so even `equals`/`toString` were dead. They are public API for wrapping a read-only collection, so tests construct them directly, which is the intended usage.
- `extensions.kt`, 48 lines: Array/Sequence/CharSequence overloads of converters and operators, deprecated `immutable*Of` aliases, deprecated `putAll` overloads, `toPersistentHashMap` dispatch chain, `intersect`.
- Interface default bodies, 10 lines: `PersistentCollection.adding` and friends only execute when called through the base interface type; `PersistentList`/`PersistentSet` re-declare them covariantly, shadowing the defaults in every existing test. Tests hold the collection in a `PersistentCollection`-typed val, which is how user code written against the base interface dispatches.
- `PersistentHashMap.entries`/`clear`/`remove(key, value)` (6), `ListImplementation.orderedHashCode/orderedEquals` (11), ordered-collection crumbs (7): thin spots reachable through ordinary calls that no test happened to make.

## Findings

1. Dead code in `immutableSet/TrieNode.kt` (6 lines): the private collision variants `mutableCollisionAddAll`/`mutableCollisionRetainAll`/`mutableCollisionRemoveAll` each start with a `this === otherNode` short-circuit (lines 235-237, 255-257, 280-282). Their only call sites (348, 450, 546) sit directly after the callers' own `this === otherNode` early returns (343, 445, 541), and recursion re-enters the outer functions, so the inner branches can never be taken. The map-side counterpart `mutableCollisionPutAll` has no such inner check, which confirms the check is redundant at that level. Candidates for removal in a follow-up; removing them would make the coverage report a literal 100%.
2. Dead guard in `immutableSet/PersistentHashSetIterator.kt` (2 lines, 47-48): reachable only when `moveToNextNodeWithData(i) == -1` while `path[i].hasNextCell()`, but -1 is returned only when frame `i` is exhausted (`hasNextCell() == false`); descents always land on non-empty stored node buffers (canonicalization lifts single elements, the only empty node is the EMPTY root). The condition is contradictory.
3. The covariant JVM accessors (`getEntries()` returning `ImmutableSet` on the map classes) are never called from Kotlin: the compiler always emits the erased `entrySet()` signature, so those accessors are Java-interop surface only. Covered by a Java test in `jvmTest`, which matches their real audience.
