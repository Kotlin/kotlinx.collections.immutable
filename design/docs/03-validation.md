# Validation

An independent pass over the campaign, done a few days after it landed: re-verify every claim in these documents against the code, review the new KDoc and samples against the implementations, run everything locally, and fix what fell out. Method: mechanical cross-checks (tag-to-sample bijection, per-file counts, additions-only diff audit, sample hygiene greps), seven scoped review passes (extensions.kt KDoc against the implementations in two halves, the interface tags, all samples in two halves, these documents, and convention conformance against kotlinx-datetime and the stdlib), with an adversarial verification vote on each finding before anything changed. 25 findings survived verification; everything below traces to one of them.

## Verified, no change needed

- The campaign as committed is green: 67/67 sample tests passed on JVM, JS, wasmJs and wasmWasi; `dokkaGeneratePublicationHtml` passed with `failOnWarning`, so all 108 original tags resolved; `checkLegacyAbi` and `checkModuleInfoExports` reported no drift.
- The interface-file diffs are additions-only, every interface tag sits on the right member, and the coverage table in [02-coverage-map.md](02-coverage-map.md) was exact, including the "75 tags, every non-deprecated top-level function" claim for `extensions.kt`.
- The five doc fixes match the implementations (`toPersistentSet`/`toPersistentMap` really short-circuit only the ordered kinds, and so on), and the Dokka wiring story in [README.md](README.md), including the quoted "common sample roots" rejection, checks out.
- The one `===` in the original set samples is backed by a real short-circuit, and no hash-variant sample asserts iteration order.

## Fixed in code

- `PersistentCollection`'s own participial members (`adding`, `addingAll`, `removing`, both `removingAll` overloads, `retainingAll`) had KDoc but no samples, while every override in the sub-interfaces had both. Six collection-typed samples were added to `PersistentCollectionSamples` and tagged; the counts moved from 67 samples / 108 tags to 73 / 114.
- `AbstractPersistentList` is public and renders in the API reference but had no KDoc, contradicting the headline of [README.md](README.md). It got a class doc, no sample. The wider question of suppressing the `implementations` packages from the docs is recorded in 02's quirks list.
- `PersistentCollection.intersect` said "Returns a new persistent set", but it returns the receiver itself when the receiver is an ordered set that loses no elements; "new" was dropped. The sibling `PersistentSet.intersect` clause could not be copied: a hash-set receiver is converted to an ordered set, so "or this instance if no modifications were made" would be false for it.
- The empty `persistentSetOf()` and `persistentMapOf()` overloads said nothing about ordering, so after doc fix 4 their pages were still indistinguishable from the hash variants when read in isolation. They now state that derived collections iterate in insertion order, mirroring the sentence the hash side got.
- The empty `persistentHashSetOf()`/`persistentHashMapOf()` overloads shared samples that never called them; both samples now end with an empty-factory call, like every ordered factory sample.
- `PersistentCollectionSamples.persistentCollection` chained `+ 3 - 1` on ints, which reads as arithmetic at first glance; it now uses strings like the rest of the file.
- Five no-op checks in the set and map samples compared with `==` where the KDoc promises "this instance" and the implementation verifiably returns the receiver (removing an absent element, `+` of a contained element, `removing(key)`, `removing(key, value)` and `minus(key)` misses). They now use `===`, so the samples demonstrate the documented identity guarantee, and rule 52 of [01-conventions.md](01-conventions.md) still holds as written.
- `ImmutableSetAdapter`'s KDoc now enumerates `equals`/`hashCode`/`toString` like its three sibling adapters.

## Fixed in these documents

- The headline promised every declaration "a runnable `@sample` snippet"; some members are deliberately sampleless and the return-type-narrowing overrides have no own KDoc at all, so it now says "the core module's non-deprecated public API".
- "Reproducing" listed `macosArm64Test`, which the campaign machine (an Intel Mac without Xcode) cannot run, and omitted `wasmWasiNodeTest` although the prose claims both Wasm targets. The block now lists exactly what runs locally, plus `checkLegacyAbi`, and points native targets at CI. The related sentence claiming "all Native targets" run on CI now says "the native targets runnable on the CI hosts": the arm-device and androidNative targets only compile the tests.
- Two rules in 01 overstated practice. The original-unchanged check appears in the persistence-focused samples, not next to every modification; and only the single-element set/map samples show the promised no-op paths. Both were reworded to describe what the samples do.
- 01's return-value-checker bullet claimed `MutableMap.remove` must be bound and checked outside `builder()`/`mutate` lambdas. Ignorability is per declaration, not per context, and `remove(key)` is `@IgnorableReturnValue` in the stdlib; the binding in the map `builder` sample is a style choice. Reworded.
- 01 attached "except the five factual fixes" to the interface files; all five fixes live in `extensions.kt` and the interface diffs have no exceptions. Split accordingly.
- README called all five applied fixes "factual gaps"; item 5 is a stale-comment removal, so it now says four gaps plus the removal.
- 02's untagged-by-design list missed `Builder.build()` in `PersistentList`/`PersistentSet` and `keys`/`values`/`entries` in `ImmutableMap`, which are untagged for the same reason as the bare `builder()` overrides it did list: no own KDoc, and a tag-only block would suppress the inherited docs.
- 02's `ImmutableSet.kt` row said "same-named functions" although the `removingAll` pair maps to two differently named samples; it now carries the same parenthetical as the list row.
- 02's `toImmutableSet` quirk said the order promise "cannot hold" on the receiver-returned-as-is path; it holds vacuously there, since the returned set is the receiver. Reworded.

## Recorded, left as is

- `PersistentMapSamples.puttingAll` is referenced from both the `puttingAll` member and the variance-driven extension; on a non-projected receiver overload resolution picks the member, so the extension's page shows code that dispatches to the member. A receiver contrived to force the extension would obscure the point. 02 already marks the sample as shared.
- `toImmutableList`'s sample does not demonstrate the returns-as-is path its doc promises, while the analogous `toPersistentList` sample does with `===`. The common-case rule covers the omission.
- `kotlin-js-store/` shows up untracked after local JS test runs; master neither tracks nor ignores it. Repo hygiene, out of scope here.

## Final state

Every command in README's "Reproducing" block was run on the campaign machine before and after the changes: 73/73 sample tests green on JVM, JS, wasmJs and wasmWasi; Dokka green under `failOnWarning`, so all 114 tags resolve; ABI dumps and `module-info` exports unchanged. In the rendered HTML, the new samples appear on the `PersistentCollection` member pages and `AbstractPersistentList` shows its class doc.
