# Module kotlinx-collections-immutable

Immutable and persistent collection types for Kotlin Multiplatform.

This library provides interfaces and efficient implementations of read-only and persistent
variants of lists, sets, and maps. It complements the Kotlin standard library, where
collection interfaces describe the absence of mutating methods on the caller but do not
guarantee that the underlying instance cannot be mutated elsewhere.

## Why immutable collections

Most Kotlin code relies on `List`, `Set`, and `Map` — interfaces that omit mutating methods
but allow the underlying implementation to be mutable. Passing such a collection across
module boundaries does not protect it from being modified by the caller or by another
holder of the same reference.

Immutable collections close this gap:

- **Safe sharing.** Once constructed, the value cannot change, so it can be freely shared
  across threads or kept as part of a state snapshot without defensive copies.
- **Predictable equality.** Hash codes are stable, which makes immutable collections safe
  to use as map keys, set elements, and cache values.
- **Stable in Compose.** Jetpack Compose recognizes persistent collections as stable, so a
  composable that takes one as a parameter can skip recomposition when its contents are
  unchanged.

## Immutable vs persistent

The library defines two interface families:

- **Immutable** — `ImmutableList`, `ImmutableSet`, `ImmutableMap`. These extend the
  standard read-only collection interfaces and add a contract that the underlying value
  cannot change after construction. They expose no operations to derive a modified copy.
- **Persistent** — `PersistentList`, `PersistentSet`, `PersistentMap`. These extend the
  immutable interfaces and add copy-returning operations such as `adding`, `removing`,
  and `putting`. The returned collection shares structure with the original, so most
  modifications run in near-constant or logarithmic time and avoid copying the entire
  collection.

Use persistent variants whenever a collection needs to evolve — to keep an event log,
accumulate intermediate results, or maintain immutable state in a reducer or UI store.
Use plain immutable variants for fully baked values that will not change.

## Quick start

```kotlin
import kotlinx.collections.immutable.*

val numbers = persistentListOf(1, 2, 3)
val extended = numbers.adding(4)                    // PersistentList: [1, 2, 3, 4]

val tags = persistentSetOf("kotlin", "immutable")
val withMore = tags.adding("multiplatform")         // PersistentSet: [kotlin, immutable, multiplatform]

val config = persistentMapOf("retries" to 3, "timeout" to 30)
val updated = config.putting("timeout", 60)         // PersistentMap: {retries=3, timeout=60}

val snapshot: PersistentList<Int> = listOf(1, 2, 3).toPersistentList()
```

The default `persistentSetOf` and `persistentMapOf` preserve element insertion order
during iteration. If iteration order does not matter, the unordered implementations
returned by `persistentHashSetOf` and `persistentHashMapOf` are more memory- and
time-efficient.

## Modifying persistent collections

The `+` and `-` operators are defined on `PersistentCollection` and `PersistentMap`.
They delegate to the collection itself and return a new persistent value. To make
them take precedence over the standard-library operators (which return plain `List`,
`Set`, or `Map`), import the package explicitly:

```kotlin
import kotlinx.collections.immutable.*

val list = persistentListOf("a", "b") + "c"         // PersistentList<String>
val map = persistentMapOf("x" to 1, "y" to 2) - "x" // PersistentMap<String, Int>
```

When several modifications are applied in sequence, `mutate { }` is more efficient
than chaining operators. It exposes a temporary builder, runs the block against it,
and returns a fresh persistent collection at the end:

```kotlin
val result = persistentListOf(1, 2, 3).mutate {
    it.add(4)
    it.removeAt(0)
    it[0] = 20
}
// result: PersistentList<Int> = [20, 3, 4]
```

The builder seen inside `mutate` implements the standard `MutableList`, `MutableSet`,
or `MutableMap` interface, so any code that already works with mutable collections can
be reused inside the block.

## Multiplatform support

`kotlinx-collections-immutable` is a Kotlin Multiplatform library. It is published for
JVM, JS, Wasm (JS and WASI), and all Kotlin/Native
[targets](https://kotlinlang.org/docs/native-target-support.html) supported by the
compiler.

# Package kotlinx.collections.immutable

The main API: immutable and persistent collection interfaces (`ImmutableList`,
`PersistentMap`, etc.), entry-point factory functions (`persistentListOf`,
`persistentSetOf`, `persistentMapOf` and their hash-based counterparts), and extension
functions for conversion (`toImmutableList`, `toPersistentSet`, …), operators (`+`, `-`),
and batch updates (`mutate`).

# Package kotlinx.collections.immutable.adapters

Adapter classes that wrap a standard read-only `Collection`, `List`, `Set`, or `Map`
and expose it through the corresponding `Immutable*` interface by delegation. Useful
when an existing collection needs to fit into an API that expects an immutable type.
Note that adapters do not freeze the underlying collection; the immutability contract
holds only if the caller guarantees the wrapped instance is not mutated elsewhere.
