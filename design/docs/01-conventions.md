# Conventions

## KDoc

Follows the [documentation-comments section of the Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#documentation-comments) and stdlib practice:

- First line is a terse summary sentence: "Returns a new persistent list with ...".
- Parameters are referenced inline with `[brackets]`; no `@param`/`@return` tags (the interface files keep their pre-existing `@param E` type-parameter docs).
- `@throws` only where the function actually throws.
- `@sample` is the last tag of the block, separated from prose by an empty ` *` line.
- Existing prose was treated as frozen: the interface files' diffs are additions-only (`@sample` lines and separators). The five fixes listed in [02-coverage-map.md](02-coverage-map.md) are all in `extensions.kt`.

Recurring phrases kept consistent across the API:

- "Returns a new persistent list/set/map with ..." for modification operations.
- "... or this instance if no modifications were made in the result of this operation." (pre-existing; kept).
- "Elements of the returned set are iterated in the order they were specified." for ordered variants.
- "Order of the elements in the returned set is unspecified." for hash variants.
- "If the receiver is already a ..., returns it as is. If the receiver is a ... builder, calls `build` on it and returns the result." for converters.

## Samples

One file per API area in `core/commonTest/src/samples/`: `PersistentListSamples.kt`, `PersistentSetSamples.kt`, `PersistentMapSamples.kt`, `PersistentCollectionSamples.kt`. One top-level class per file, one `@Test fun` per sample; the reference format is `@sample kotlinx.collections.immutable.samples.<Class>.<function>`. Dokka renders only the function body.

The canonical shape:

```kotlin
@Test
fun mutate() {
    val fruits = persistentListOf("apple", "banana")
    val updated = fruits.mutate {
        it.add("cherry")
        it.remove("apple")
    }
    check(updated == listOf("banana", "cherry"))
    check(fruits == listOf("apple", "banana"))
}
```

Rules:

- Bodies are 3–10 lines, self-contained, and show the common use case, not corner cases. Descriptive local names (`fruits`, `updated`, `version1`), small literals. Imports are only `kotlinx.collections.immutable.*` and `kotlin.test.Test`.
- No comments anywhere in a sample file below the copyright header, and no `@Suppress` (both would render in the docs).
- Persistence is demonstrated by re-checking the original next to the modified copy where the sample's point is persistence itself: the class-level samples, `mutate`, `builder`, and everything in `PersistentCollectionSamples`. Short samples focused on the result's shape omit the check.
- Single-element addition/removal samples for sets/maps show the no-op path where the docs promise one (adding a contained element or removing an absent one compares `===` to the original); bulk-operation samples show only the effective path.

Constraints imposed by the build (the module compiles test sources with `-Werror` and `-Xreturn-value-checker=full`):

- Every value a sample declares must be used; every result of a persistent-collection call must be bound and checked. Discarding results of the common `MutableCollection`/`MutableMap` mutation methods (`add`, `remove`, `put`, `remove(key)`) is fine anywhere — they are `@IgnorableReturnValue` in the stdlib. The map `builder` sample binds and checks `remove`'s result to show the returned previous value; that is a style choice, not a checker requirement.
- A sample named after a factory shadows it for unqualified zero-argument calls inside its own body (`fun persistentListOf()` vs `persistentListOf()`), so empty-factory calls always pass explicit type arguments: `persistentListOf<String>()`.
- Iteration-order assertions are allowed only for the ordered kinds; hash-based results are checked with `size`, `in`, lookups, and `==` against `setOf`/`mapOf`, which is order-independent.
- `===` only where the KDoc promises "this instance" and the short-circuit was verified in the implementation; if such a check ever proves platform-sensitive, weaken it to `==`.
