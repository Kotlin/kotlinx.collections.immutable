# Gap analysis

Why 575 lines had no deterministic coverage, grouped by cause. The three buckets below are prose groupings and overlap; the exact 575-line partition is the table in [03-test-plan.md](03-test-plan.md). Line-level detail lives in the baseline report; regenerate with `koverHtmlReport -PjvmTestExcludes=tests.stress.*`.

## Large-list trie machinery, 408 lines

`PersistentVectorBuilder.kt` (188), `PersistentVector.kt` (118), `SmallPersistentVector.kt` (10), the list iterators (41), and their helpers. Everything that only runs once a list crosses 32 elements and grows a root trie: tail push with height growth, insert-into-root with element carry, `addAll(index, ...)` buffer splitting, remove with buffer pull and height decrease, `set` in the root, backward iteration across the tail/trie boundary. The deterministic suite simply never built such lists: Guava testlib works on sizes 0/1/3, and the contract tests stay small. Only the stress tests went big, nondeterministically.

The immutable `PersistentVector` paths (118 lines) were even thinner than the builder paths: contract tests that do grow lists do it through builders, so the pure persistent `adding`/`add(index)`/`removeAt`/`set` recursions were exercised almost nowhere.

## Hash-collision and two-arg-remove paths, 90 lines

`immutableMap/TrieNode.kt` (58), `immutableSet/TrieNode.kt` (25), plus small iterator pieces. Three ingredients are rare in the existing tests: full-hash collision nodes built deterministically (existing collision tests are stress-based), `remove(key, value)` / `remove(element)` variants that compare values before removing, and node-canonicalization on removal (single-child collapse). The `putAll` merge short-circuits for reference-equal subtrees also live here.

## Never-called public API, 103 lines

- `adapters/ReadOnlyCollectionAdapters.kt`, 17 lines: the whole package. No factory creates adapters and no test instantiated one, so even `equals`/`toString` were dead. They are public API for wrapping a read-only collection, so tests construct them directly, which is the intended usage.
- `extensions.kt`, 48 lines: Array/Sequence/CharSequence overloads of converters and operators, deprecated `immutable*Of` aliases, deprecated `putAll` overloads, `toPersistentHashMap` dispatch chain, `intersect`.
- Interface default bodies, 10 lines: `PersistentCollection.adding` and friends execute only for implementations that do not override them. The library's own classes all override covariantly, so no test against them can reach the defaults; the defaults serve third-party implementations written against the pre-0.5.0 abstract methods. The tests model exactly that: a legacy wrapper overriding only the deprecated members.
- `PersistentHashMap.entries`/`clear`/`remove(key, value)` (6), `ListImplementation.orderedHashCode/orderedEquals` (11), ordered-collection crumbs (7): thin spots reachable through ordinary calls that no test happened to make.

## Findings

1. Dead code in `immutableSet/TrieNode.kt` (6 lines): the private collision variants `mutableCollisionAddAll`/`mutableCollisionRetainAll`/`mutableCollisionRemoveAll` each start with a `this === otherNode` short-circuit; the uncovered lines are the guard bodies (236-237, 256-257, 281-282; the `if` lines themselves are covered). Their only call sites (348, 450, 546) sit directly after the callers' own `this === otherNode` early returns (343, 445, 541), and recursion re-enters the outer functions, so the inner branches can never be taken. The map-side counterpart `mutableCollisionPutAll` has no such inner check, which confirms the check is redundant at that level. Candidates for removal in a follow-up; removing them would make the coverage report a literal 100%.
2. Defensive recovery in `immutableSet/PersistentHashSetIterator.kt` (2 lines, 47-48): runs only when a descent lands in a stored child node with an empty buffer, a state canonicalization never produces (single elements are lifted; the only empty node is the shared EMPTY root). Unlike finding 1, the state is constructible from tests through the internal constructors, and the map iterator has the identical recovery with exactly such a hand-built test, so the validation covered these two lines the same way (`implementations/set/SetContentIteratorsTest.kt`) instead of declaring them dead; see [06-validation.md](06-validation.md).
3. The covariant JVM accessors (`getEntries()` returning `ImmutableSet` on the map classes) are never called from Kotlin: the compiler always emits the erased `entrySet()` signature, so those accessors are Java-interop surface only. Covered by a Java test in `jvmTest`, which matches their real audience.
