# Verification

## Final coverage

| suite | line | branch | missed lines |
|---|---|---|---|
| deterministic (`-PjvmTestExcludes=tests.stress.*`) | 2812/2820 (99.72%) | 89.94% | 8 |
| full, including stress | 2812/2820 (99.72%) | 90.77% | 8 (the same) |

The deterministic subset and the full suite now miss exactly the same 8 lines: nothing depends on stress-test luck anymore (at baseline the two differed by 437 lines). Branch coverage is reported, not gated; the remaining partial branches are dominated by defensive `assert()`/`check()` conditions that cannot fail without corrupted internal state.

`koverVerify` enforces the result as a missed-lines bound of at most 8 and was negative-tested (a bound of 7 fails the build).

## The 8 uncovered lines are dead code

All 8 sit in the set trie and are unreachable by construction, not just untested; each was verified against the source, and none can be reached from tests (the enclosing functions are private).

1. `immutableSet/TrieNode.kt` 236-237, 256-257, 281-282: `this === otherNode` short-circuits inside the private `mutableCollisionAddAll`/`mutableCollisionRetainAll`/`mutableCollisionRemoveAll`. Each function's only call site (lines 348, 450, 546) is guarded by the caller's own `this === otherNode` early return (343, 445, 541), and the recursion between levels re-enters the guarded outer functions, so the inner checks never fire. The hash-map counterpart (`mutableCollisionPutAll`) has no such inner check.
2. `immutableSet/PersistentHashSetIterator.kt` 47-48: reachable only when `moveToNextNodeWithData(i) == -1` while `path[i].hasNextCell()`. -1 is returned only when frame `i` is exhausted (`hasNextCell() == false`): a descent always lands at index 0 of a stored node's buffer, and stored nodes are never empty (canonicalization lifts single elements; the only empty node is the shared EMPTY root). The guard is contradictory.

Removing these 8 lines is a production cleanup worth a follow-up; with them gone the report is a literal 100%. No Kover exclusion is used: the finest filter granularity is a whole class, which would hide the ~800 covered lines of `TrieNode`/`PersistentHashSetIterator`.

## Review outcomes

Three independent reviewers (list / map / shared surfaces) checked every new test for behavioral substance, correctness of expectations against the production sources, duplication against the existing suite, and conventions. No must-fix findings. Applied outcomes:

- White-box tests moved out of the contract packages into `tests.implementations.map` (`MapContentIteratorsTest`, `HashMapTrieNodeExtraTest`) and into the existing `tests.implementations.list.TrieIteratorTest` (one added method).
- The two builder-entry `setValue` iteration tests now collect yields into a list, so a broken `resetPath` re-yielding visited entries would fail them (previously a map collapse could mask duplicates).
- One redundant ordered-set iterator test was dropped: `iteratorBehavior` in the existing contract suite already covers insertion-order iteration and the past-the-end exception.
- A `removeAll` test moved from the insertion file to the removal file; a builder-layout KDoc was corrected (sizes 33..64 keep a single-leaf root with rootShift = 0).

## Cross-platform runs

The new tests are `commonTest` (plus one Java `jvmTest` file) and were run on: JVM (full suite incl. stress), JS (Node), WasmJS (Node), WasmWASI (Node), macosArm64. One JS-only failure surfaced during verification: comparing a stdlib map against a test-fixture wrapper that defines no `equals` behaves differently on JS than on the JVM; the assertion now compares content (`toMap()`), which is the actual contract under test. All suites green after the fix.
