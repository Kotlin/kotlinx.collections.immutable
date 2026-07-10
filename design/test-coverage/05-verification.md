# Verification

Superseded in part by [06-validation.md](06-validation.md): the numbers below describe the campaign's delivered state; the validation reproduced them all, then tightened the result to 2821/2827 lines (6 missed), 94.87% branches, and fixed two production bugs.

## Final coverage

| suite | line | branch | missed lines |
|---|---|---|---|
| deterministic (`-PjvmTestExcludes=tests.stress.*`) | 2812/2820 (99.72%) | 89.94% | 8 |
| full, including stress | 2812/2820 (99.72%) | 90.77% | 8 (the same) |

The deterministic subset and the full suite now miss exactly the same 8 lines: nothing depends on stress-test luck anymore (at baseline the two differed by 437 lines). Branch coverage is reported, not gated. The validation later classified every missed branch and disproved the first draft of this paragraph: only about a fifth of the misses were defensive guards, half were untested reachable behavior (two of those branches hid real bugs), and the rest dead sides or compiler artifacts. After closing the reachable ones the remainder is defensive, dead, or artifact, plus four named marginal slots ([06-validation.md](06-validation.md)).

`koverVerify` enforces the result as a missed-lines bound and was negative-tested (a bound of 7 failed the build at 8 missed; the validation re-verified the gate by exclusion and tightened the bound to 6).

## The uncovered lines are dead code

All sit in the set trie and are unreachable through the public API; each was verified against the source (and re-proved independently during validation). The six in `TrieNode.kt` cannot be reached from tests at all without JVM reflection: the enclosing functions are private. The iterator pair could be, see item 2.

1. `immutableSet/TrieNode.kt` 236-237, 256-257, 281-282: `this === otherNode` short-circuits inside the private `mutableCollisionAddAll`/`mutableCollisionRetainAll`/`mutableCollisionRemoveAll`. Each function's only call site (lines 348, 450, 546) is guarded by the caller's own `this === otherNode` early return (343, 445, 541), and the recursion between levels re-enters the guarded outer functions, so the inner checks never fire. The hash-map counterpart (`mutableCollisionPutAll`) has no such inner check.
2. `immutableSet/PersistentHashSetIterator.kt` 47-48: runs only when a descent lands in a stored child node with an empty buffer, which canonicalization never produces (single elements are lifted; the only empty node is the shared EMPTY root). Unlike item 1, that state is constructible from tests through the internal constructors, and the map iterator's identical recovery already had such a hand-built test, so the validation added the mirror (`implementations/set/SetContentIteratorsTest.kt`). Only the six lines of item 1 remain uncovered.

Removing the six `TrieNode.kt` lines is a production cleanup worth a follow-up; with them gone the report is a literal 100%. No Kover exclusion is used: a class-level filter would hide the ~800 covered lines of `TrieNode`, and an annotation-based one would require annotating production code.

## Review outcomes

Three independent reviewers (list / map / shared surfaces) checked every new test for behavioral substance, correctness of expectations against the production sources, duplication against the existing suite, and conventions. No must-fix findings. Applied outcomes:

- White-box tests moved out of the contract packages into `tests.implementations.map` (`MapContentIteratorsTest`, `HashMapTrieNodeExtraTest`) and into the existing `tests.implementations.list.TrieIteratorTest` (one added method).
- The two builder-entry `setValue` iteration tests now collect yields into a list, so a broken `resetPath` re-yielding visited entries would fail them (previously a map collapse could mask duplicates).
- One redundant ordered-set iterator test was dropped: `iteratorBehavior` in the existing contract suite already covers insertion-order iteration and the past-the-end exception.
- A `removeAll` test moved from the insertion file to the removal file; a builder-layout KDoc was corrected (sizes 33..64 keep a single-leaf root with rootShift = 0).

## Cross-platform runs

The new tests are `commonTest` (plus one Java `jvmTest` file) and were run on: JVM (full suite incl. stress), JS (Node), WasmJS (Node), WasmWASI (Node). The originally claimed macosArm64 run could not have executed tests: the working machine is an Intel Mac without full Xcode, where arm64 test binaries cannot run and native test tasks cannot even link, so a green build at best means the task was skipped ([06-validation.md](06-validation.md)). Executing the suite on a native target remains to be done on a machine with Xcode; CI compiles the native targets on all three hosts. One JS-only failure surfaced during verification: comparing a stdlib map against a test-fixture wrapper that defines no `equals` behaves differently on JS than on the JVM; the assertion now compares content (`toMap()`), which is the actual contract under test. All suites green after the fix.
