# Coverage map

Mechanical necessity evidence for the new tests. Baseline and final refer to the deterministic-subset Kover reports before/after the campaign.

## Diff

Of the 575 lines missed at baseline, 567 are covered by the new tests and 8 remain missed; those 8 are the provably dead lines analyzed in [05-verification.md](05-verification.md). No line that was covered at baseline regressed. Every new test therefore covers lines no pre-existing test covered deterministically, which is the necessity argument against the old suite. (Two more line records flagged at baseline turned out to be Kover phantom entries with zero instructions, see the measurement note below.)

## New test files -> production lines uniquely covered

Layout after review (white-box tests were moved from the contract packages to `tests.implementations.*`, one redundant ordered-set iterator test was dropped, and one removeAll test moved from the insertion to the removal file):

| new test file | tests | production territory (missed lines closed) |
|---|---|---|
| `contract/list/PersistentListBuilderInsertionTest.kt` | 8 | PersistentVectorBuilder add/insert/addAll machinery (~116 incl. SingleElementListIterator) |
| `contract/list/PersistentListBuilderRemovalTest.kt` | 11 | PersistentVectorBuilder get/set/remove/removeAll machinery (~79) |
| `contract/list/PersistentVectorTest.kt` | 8 | PersistentVector + SmallPersistentVector + AbstractPersistentList + Utils (~138) |
| `contract/list/PersistentListIteratorTest.kt` | 3 | public list-iterator walks: PersistentVector(Mutable)Iterator, TrieIterator via builders (~32) |
| `contract/map/PersistentHashMapExtraTest.kt` | 7 | map TrieNode collision/two-arg-remove paths + PersistentHashMap (~58) |
| `contract/map/PersistentMapEntryIteratorTest.kt` | 5 | builder entry setValue machinery + interface-typed entries access (~20) |
| `implementations/map/MapContentIteratorsTest.kt` | 4 | content iterators: TrieNodeBaseIterator contract, empty-child skip, ordered links iterators (~9) |
| `implementations/map/HashMapTrieNodeExtraTest.kt` | 2 | defensive canonicalization on hand-built single-entry sub-nodes (~5) |
| `src/jvmTest/java/.../PersistentMapJavaAccessorsTest.java` | 1 | covariant getEntries accessors of both map classes (2, Java-only call sites) |
| `contract/ConversionsTest.kt` + `contract/DeprecatedApiTest.kt` | 17 | extensions.kt overloads/aliases + interface default bodies (~58) |
| `contract/set/PersistentHashSetExtraTest.kt` + `contract/set/PersistentOrderedSetExtraTest.kt` | 6 | set TrieNode collision paths, set iterators, ordered set retainAll (~26) |
| `contract/AdaptersTest.kt` | 5 | the whole adapters package (17) |
| `implementations/InternalUtilitiesTest.kt` | 2 | ListImplementation.orderedHashCode/orderedEquals (11; zero production callers, tested as the internal contract for list implementors) |

One method (`previousAtLeftEdgeTest`) was added to the pre-existing `implementations/list/TrieIteratorTest.kt`, next to its forward-iteration siblings. No pre-existing test was modified or removed anywhere.

Per-test scenarios are in the test names (sentence style); per-test line claims were tracked during the campaign and verified by the diff above.

## Knockout checks

Re-measured coverage with one new test class excluded at a time (`-PjvmTestExcludes=tests.stress.*,<class>`); its territory must regress to missed:

| knocked out | regressed lines | where |
|---|---|---|
| `PersistentVectorTest` | 125 | PersistentVector 107, SmallPersistentVector 10, AbstractPersistentList 5, Utils 3 |
| `AdaptersTest` | 17 | ReadOnlyCollectionAdapters 17 (the entire package) |
| `PersistentHashMapExtraTest` | 63 | map TrieNode 58, PersistentHashMap 5 |

Each sampled file's claimed territory collapses without it and nothing else covers it.

## Measurement note

A Kover XML `line` record can carry `mi=0 ci=0` (no instructions at all); two such phantom records (first statements of `next()` methods behind interface bridges) initially looked like missed lines. A line is missed iff `ci == 0` and `mi > 0`. Kover's own LINE totals already exclude phantom records.
