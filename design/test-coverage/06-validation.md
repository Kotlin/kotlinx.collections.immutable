# Validation

An independent pass over the campaign: every number in 01-05 was re-measured, every argument re-derived from source, every new test re-reviewed, and the branch-coverage gaps the campaign left ungated were classified line by line. Environment: Intel MacBook Pro (macOS 26.5), Temurin JDK 21, Gradle 8.10, Kotlin 2.3.0, Kover 0.9.8.

The campaign's measurements all reproduce. Its documentation needed a handful of factual fixes (applied in place, listed below). Its "branch coverage is defensive-only" claim did not hold, and following the untested branches uncovered two shipped production bugs, both fixed here. The final state is stronger than the delivered one:

| | delivered | validated final |
|---|---|---|
| line coverage (deterministic subset) | 2812/2820 (99.72%), 8 missed | 2821/2827 (99.79%), 6 missed |
| missed lines | 6 dead + 2 arguably coverable | only the 6 provably dead |
| branch coverage | 1394/1550 (89.94%) | 1478/1558 (94.87%) |
| method / class coverage | 100% / 100% | 100% / 100% |
| `koverVerify` bound | at most 8 missed lines | at most 6 |
| production bugs known | 0 | 2 found, 2 fixed |

## Reproduction of the campaign's numbers

All runs on the delivered branch head (23bf970), before any correction:

| claim | result |
|---|---|
| deterministic subset: 2812/2820, 8 missed | reproduced exactly; the 8 missed lines are byte-for-byte the documented set |
| determinism | two runs with `cleanJvmTest` in between produced byte-identical report XML |
| full suite: same lines, branch 90.77% | same 2812/2820 and the same 8 lines; branch measured 90.84%, the stress randomness moves the full-run branch figure between runs (the deterministic 89.94% reproduces exactly) |
| baseline on master: 2245/2820, 575 missed in 26 files | reproduced exactly in a worktree at 763b960 |
| `koverVerify` gate | passes at bound 8; with `PersistentVectorTest` also excluded it fails with "lines missed count is 133, but expected maximum is 8", which proves the gate live without editing the build |
| knockout: PersistentVectorTest -> 125 | reproduced exactly, including the 107/10/5/3 per-file split |
| knockout: AdaptersTest -> 17 | reproduced exactly |
| knockout: PersistentHashMapExtraTest -> 63 | stale: measured 48. The white-box canonicalization tests were moved into `HashMapTrieNodeExtraTest` after the knockout was taken; that file's unique territory is the other 15 lines, and knocking out both regresses exactly the documented 63. 04 now records the final-layout split |
| Java accessors test executed | yes: 1 test, 0 failures, in the jvmTest results |
| 205 deterministic / 278 full jvmTest runs green | yes (now 293 full after the validation's changes) |

## The dead-lines argument, sharpened

A blind prover (given only the eight locations, not the docs' argument) and two adversarial reviewers of its proof all reached the same verdict:

- `immutableSet/TrieNode.kt` 236-237, 256-257, 281-282 are dead. The three collision variants are private, each has exactly one call site, and each call site is dominated by the caller's own identity check; recursion re-enters the guarded outer functions, so a structurally shared identical subtree is caught one level up. No test can reach them without JVM reflection. These six stay uncovered and remain the gate's allowance.
- `PersistentHashSetIterator.kt` 47-48 were not dead in the documented sense. 05 claimed "none can be reached from tests (the enclosing functions are private)", but the class and the `TrieNode` constructor are internal: a hand-built trie with an empty stored child executes both lines. That is exactly the technique `MapContentIteratorsTest` already uses for the map iterator's identical recovery, and the campaign's own policy ("code unreachable through the public API gets a direct internal-API test") points the same way. The validation added the mirror test (`implementations/set/SetContentIteratorsTest.kt`), so the report's uncovered remainder is now exactly the provably dead six, and `koverVerify` is tightened to 6.

## Two production bugs, found and fixed

Both came out of classifying the missed branches the campaign left ungated. Both predate the campaign (verified by running probes in a worktree on untouched master code) and affect all platforms.

1. `putAll` dropped incoming values for hash-colliding keys. `mutableCollisionPutAll` (immutableMap/TrieNode.kt) copied only the keys absent from the target, so for a common key the stale value survived, except when the target's colliding keys were a subset of the incoming ones, in which case the incoming node was returned and the values did update. Inconsistent with the entry-merge path in the same file, with the plain-`Map` argument fallback, and with the `putAll` contract. Introduced by 690343a (Faster addAll/putAll implementation, #86), so it has shipped in every release since. Fixed by overwriting the value in place (reference-identity check, matching `collisionPut`); the identity fast path still returns `this` when nothing changed. Regression tests: the two `putting all ...` tests in `PersistentHashMapExtraTest`.

2. `removeAll` lost the last 32 survivors behind nulls. In `PersistentVectorBuilder.removeAll(predicate)`, whenever the removal touched the root trie and the survivor count was an exact positive multiple of 32, the empty working buffer was installed as the tail, violating the invariant that a trie-backed vector keeps its last leaf in the tail. Reads of the last 32 elements returned null (an NPE after unboxing); the builder and the immutable `removingAll` are the same code path. Repro: `(0..1088).toPersistentList().removingAll { it >= 1024 }` then read element 1000. Fixed by pulling the last leaf back out of the root (the same `pullLastBufferFromRoot` the tail-only path already used). Regression test: `removeAll retaining an exact multiple of the leaf size keeps the last leaf reachable` in `PersistentListBuilderRemovalTest`, covering the full-child, partial-child, interleaved-survivor and single-level shapes plus the immutable path.

The second bug is the sharper lesson for the campaign's methodology: the line was covered, the test suite was green, and only the untested branch side hid the data loss.

## Test-quality adjudication

Three area auditors re-reviewed every new test method; every finding that proposed a change was then attacked by an adversarial verifier, and every removal was additionally checked empirically (coverage re-measured after the removal; the missed set must stay exactly the dead lines). Outcomes:

- Removed as redundant (confirmed by verifier and by measurement): the builder `get`-descent test in `PersistentListBuilderRemovalTest` (its walk duplicates `assertBuilderContents` on the same shape, the guard is pre-covered), the two "entries expose contents" tests in `PersistentHashMapExtraTest` and `PersistentMapEntryIteratorTest` (a concrete-receiver cast does not change dispatch, so neither exercised a path the remaining tests miss; the Java test alone pins the covariant `getEntries` bridge), and the `retainingAll` delegation test in `DeprecatedApiTest` (every behavior pinned elsewhere by the same suite).
- Removed, then partially restored: the two ordered-map links-iterator tests duplicated pre-covered iteration, order, and exception behavior (the builder CME even by guava-testlib), but measurement showed their `index` reads were the only coverage of both iterators' `index` accessors, whose getters have no production callers. A single focused test (`links iterators drive hasNext from their index bookkeeping`) now pins exactly that contract and nothing else.
- Kept against a removal proposal: `cleared persistent hash map is the empty map` (the verifier refuted the redundancy claim).
- Strengthened: the `LegacyPersistentMap` fixture exercised only two of the five `PersistentMap` gerund defaults; `removing(key, value)`, `puttingAll` and `cleared` are now asserted through it too.
- Comment fixes: the removal-file KDoc capped its two-level claim at the next promotion (~32800), and the `removeAll ... nullifies` test name no longer claims root-level nullification its shape cannot produce.

The rest of the suite held up: bit-level collision setups verified against the trie code, boundary sizes verified against the constants, platform-sensitive assertions absent, and the hand-built map tries confirmed as the right technique for states the public API cannot produce.

## Branch coverage: from "defensive-only" to classified

05 said the remaining partial branches were "dominated by defensive assert()/check() conditions". Classifying all 156 missed branches disproved that: about 22% were defensive, and 80 branches (51%) were reachable public-API behavior with observable outcomes. The validation added 21 test methods keyed to those findings (converter fast paths, minus/intersect with plain iterables, builder-argument and owned-builder bulk set operations, collision-merge shapes, null elements and keys throughout, big-vector boundary insert/remove, two-arg remove key mismatches, map structural-equality false paths, builder self-equality, the ordered builder's setValue-after-remove divergence), which also flushed out the two bugs above.

Result: 1478/1558 branches (94.87%). The remaining 80 all have names: roughly 35 defensive guard sides that need corrupted state, 9 statically dead sides (the six dead lines' conditions and the single-entry-collision canonicalization arms), 4 sides unreachable on the JVM because the compiler-generated entry-set bridge filters first, ~28 compiler/inlining artifacts with no semantic side (inlined `filterTo` slots, step-loop codegen, an assertion-availability probe), and 4 marginal real slots not worth a contrived test: one slot each in `PersistentVector.kt:227` and `PersistentVectorBuilder.kt:676` (a pull shape not constructed by any natural scenario tried), the null side of the deprecated `PersistentHashSet.remove` (the replacement `removing` is covered with null), and one slot of the owned-reuse condition in the map's `mutablePutAll`. Branch coverage stays reported, not gated; the honest summary is now "the remaining misses are defensive, dead, or codegen, plus four named marginal slots", and that sentence replaces the wrong one in 05.

## Cross-platform runs

Final state, this machine: JVM 293/293 (full suite including stress), JS Node 282/282, WasmJS Node 282/282, WasmWASI Node 282/282. All green.

05's claim of a macosArm64 run cannot have been a test execution: this is an Intel machine, and arm64 test binaries do not run on Intel hosts (the Gradle task is skipped as non-runnable, so a green build proves nothing). Independently, Kotlin/Native linking requires full Xcode (`xcrun xcodebuild -version`), which this machine does not have, so no Apple-native test task can run here at all; a native run would have failed, not passed. 05 now records the verifiable set. Native targets remain compile-checked by CI on all three hosts; executing the new common tests on a native target needs a machine with Xcode.

## Doc corrections applied

- README: final numbers and the 6-line allowance; "577" -> 575 line records plus 2 Kover phantoms; the gate sentence no longer claims to pin specific lines; the exclusion note no longer claims class granularity is Kover's finest (annotation filters are finer); the scope note says plainly that native targets were not executed.
- 01-baseline: "missed iff ci is 0" completed with "and mi > 0" (the phantom-record rule 04 already documented); "never builds a list past 32 elements" softened to match the evidence.
- 02-gap-analysis: the bucket sizes are labeled as overlapping prose (they sum past 575; the exact partition is 03's table); finding 1 cites the missed bodies (236-237, 256-257, 281-282), not the covered `if` lines; finding 2 rewritten for the empty-child reachability and the mirror test; the interface-default bullet now describes dispatch correctly (defaults run for implementors that do not override them, which is what the legacy-wrapper test models).
- 04-coverage-map: the map knockout row records the final-layout split (48 + 15, union 63); `HashMapTrieNodeExtraTest`'s territory corrected from ~5 to 15; the entry-iterator row no longer credits "interface-typed access" as a distinct dispatch path.
- 05-verification: superseded-numbers pointer to this file; the dead-lines justification split into the domination argument (the six) and the corrected story for the iterator pair; the branch-coverage sentence replaced; the cross-platform list corrected.
- core/build.gradle.kts: gate comment rewritten (count bound, not identity), bound tightened to 6.

## Reproducing

Same commands as the README, now expecting at most 6 missed lines:

```
./gradlew :kotlinx-collections-immutable:koverVerify -PjvmTestExcludes=tests.stress.*
./gradlew :kotlinx-collections-immutable:koverHtmlReport -PjvmTestExcludes=tests.stress.*
```

Parsing note, same as 04: a line is missed iff `ci == 0 && mi > 0`; two phantom records with zero instructions exist in every report.

## Left open

- The four marginal branch slots named above.
- `TrieIteratorTest.simpleTest` draws leaf counts from an unseeded Random inside the measured subset. Empirically harmless (byte-identical reports across runs, any drawn count walks the same code), so it was left alone; worth a seed if it ever wobbles.
- Applying the Kover plugin instruments every `jvmTest` run (the binary report is written even for plain `check`); `onCheck = false` only detaches the verification rule. Harmless today, but worth knowing when reading CI timings.
- CI does not run `koverVerify`; the gate is manual, as the campaign chose. Wiring it into CI is a maintainer decision.
- The six dead lines are still the follow-up production cleanup 05 proposed; with them gone the report is a literal 100%.
