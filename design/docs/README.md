# Documentation and samples campaign

Part of the stabilization effort: give every non-deprecated public declaration of the core module concise KDoc and a runnable `@sample` snippet, following the conventions of the Kotlin standard library and kotlinx-datetime.

Result: 67 sample functions in `core/commonTest/src/samples/` referenced by 108 `@sample` tags — 75 in `extensions.kt` (every non-deprecated top-level function) and 33 across the four interface files (class-level docs, the participial members, the builder workflow). The four adapter classes in `kotlinx.collections.immutable.adapters`, previously undocumented, got class KDoc. Five factual gaps in existing docs were fixed along the way (see [02-coverage-map.md](02-coverage-map.md)).

## Infrastructure

Samples live inside the existing `commonTest` source set (`core/commonTest/src/samples/`, package `kotlinx.collections.immutable.samples`), so they compile and their `check(...)` assertions run as ordinary tests on every target — JVM, JS, both Wasm targets, and all Native targets — on every CI run. Dokka embeds the same functions into the rendered API reference, and the already-applied `kotlin-playground-samples-plugin` makes them editable and runnable in the browser.

The Dokka wiring existed before this campaign but was a silent no-op: `samples.from("$platform/test")` was copied from kotlinx-datetime, whose source-set directory convention (`common/test`) differs from this repo's (`commonTest/src`), so it pointed at directories that never existed. It now points at `commonTest/src/samples`, registered on the `commonMain` Dokka source set only — all `@sample` tags live in `commonMain`, and Dokka rejects the same sample root registered on more than one source set ("Source sets X and Y have the common sample roots"), which is also why kotlinx-datetime's per-platform paths had to be distinct. `failOnWarning = true` in the Dokka publication means an unresolvable `@sample` reference fails the docs build, which keeps the tags honest.

## Key decisions

- One sample per overload cluster: the `Iterable`/`Array`/`Sequence` overloads of an operation share a single sample (stdlib practice), while distinct semantics (`plus(pair)` vs `plus(map)`, `removing(key)` vs `removing(key, value)`) get distinct samples.
- Assertions are stdlib `check(x == y)` against `listOf`/`setOf`/`mapOf` literals, as in kotlinx-datetime. No `assertEquals`, no custom helpers, no `println`.
- No comments inside sample bodies — a deliberate deviation from kotlinx-datetime, which opens each sample with a `//` summary. The snippet under the documentation prose should not repeat it.
- Samples for the hash-based variants (`persistentHashSetOf`, `persistentHashMapOf`, `toPersistentHashSet`, `toPersistentHashMap`) never assert iteration order; samples for the ordered variants assert it deliberately (`.toList()`, `.keys.toList()`), since insertion-order iteration is part of their contract.
- Referential `===` checks appear only where the docs promise "this instance" and the implementation verifiably short-circuits (duplicate `adding` on an ordered set, `toPersistentList` on a persistent receiver). Everything else uses `==`.
- Adapters got KDoc stating the no-copy, caller-guarantees-immutability contract, but no samples: an example would either be trivial or normalize wrapping something mutable.
- Deprecated API (the `add`/`remove`/`put` mimics, `immutable*Of`, `putAll`) was left untouched: `skipDeprecated = true` hides it from the docs anyway.

## Reproducing

```
./gradlew :kotlinx-collections-immutable:jvmTest --tests "kotlinx.collections.immutable.samples.*"
./gradlew :kotlinx-collections-immutable:jsNodeTest :kotlinx-collections-immutable:wasmJsNodeTest :kotlinx-collections-immutable:macosArm64Test
./gradlew :kotlinx-collections-immutable:dokkaGeneratePublicationHtml   # fails on any unresolved @sample
```

## Contents

- [01-conventions.md](01-conventions.md): the KDoc and sample style rules applied, with the constraints that shaped them.
- [02-coverage-map.md](02-coverage-map.md): declaration-to-sample map, doc fixes applied, known quirks left as is.
