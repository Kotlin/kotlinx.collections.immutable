# Module kotlinx-collections-immutable

Immutable and persistent collection types for Kotlin Multiplatform.

This library provides interfaces and efficient implementations of read-only and persistent
variants of lists, sets, and maps. It complements the Kotlin standard library, where
collection interfaces describe the absence of mutating methods on the caller but do not
guarantee that the underlying instance cannot be mutated elsewhere.

## Why immutable collections

Most Kotlin code relies on [`List`][kotlin.collections.List], [`Set`][kotlin.collections.Set],
and [`Map`][kotlin.collections.Map] — read-only interfaces that prevent the receiver from
modifying the collection but do not constrain the underlying implementation. If the caller
still holds a [`MutableList`][kotlin.collections.MutableList] reference to the same
instance, it can mutate the list from outside at any time — even while the function it
was passed to is still using it. The receiving function therefore cannot assume the
contents stay stable for its lifetime, and so cannot, for example, cache the value or
share it across threads. A value obtained from
[`listOf`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/list-of.html)
can be relied on in practice, but that reliance
rests on convention rather than on any guarantee carried by the type.

Immutable collections close this gap:

- **Safe sharing.** Once constructed, the value cannot change, so it can be freely shared
  across threads or kept as part of a state snapshot without defensive copies.
- **Stable equality and hashing.** Because the collection contents never change, `equals`
  and `hashCode` stay consistent as long as element equality and hashing stay stable — so
  immutable collections are safe to use as map keys, set elements, or cache values under
  that condition.
- **Friendly to memoization.** Frameworks that skip work when inputs are unchanged rely on
  this stability. [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/),
  for example, recognises [`ImmutableList`][kotlinx.collections.immutable.ImmutableList],
  [`ImmutableSet`][kotlinx.collections.immutable.ImmutableSet], and
  [`ImmutableMap`][kotlinx.collections.immutable.ImmutableMap] as stable types, letting it
  skip recomposition when contents don't change.

## Immutable vs persistent

The library defines two interface families:

- **Immutable** — [`ImmutableList`][kotlinx.collections.immutable.ImmutableList],
  [`ImmutableSet`][kotlinx.collections.immutable.ImmutableSet],
  [`ImmutableMap`][kotlinx.collections.immutable.ImmutableMap]. These extend the standard
  read-only collection interfaces and add a contract that the underlying value cannot
  change after construction. They expose no operations to derive a modified copy.
- **Persistent** — [`PersistentList`][kotlinx.collections.immutable.PersistentList],
  [`PersistentSet`][kotlinx.collections.immutable.PersistentSet],
  [`PersistentMap`][kotlinx.collections.immutable.PersistentMap]. These extend the
  immutable interfaces and add copy-returning operations such as
  [`adding`][kotlinx.collections.immutable.PersistentCollection.adding],
  [`removing`][kotlinx.collections.immutable.PersistentCollection.removing],
  [`putting`][kotlinx.collections.immutable.PersistentMap.putting]. The returned
  collection shares structure with the original, so most modifications run in
  near-constant or logarithmic time and avoid copying the entire collection.

Use persistent variants whenever a collection needs to evolve — to keep an event log,
accumulate intermediate results, or hold collection-shaped state in containers that update
through successive immutable snapshots (Redux- or MVI-style). Use plain immutable variants
for fully baked values that will not change.

## Quick start

```kotlin
import kotlinx.collections.immutable.*

val numbers = persistentListOf(1, 2, 3)
val extended = numbers.adding(4)                    // PersistentList: [1, 2, 3, 4]

val tags = persistentSetOf("kotlin", "immutable")
val withMore = tags.adding("multiplatform")         // PersistentSet: [kotlin, immutable, multiplatform]

val config = persistentMapOf("retries" to 3, "timeout" to 30)
val updated = config.putting("timeout", 60)         // PersistentMap: {retries=3, timeout=60}

val snapshot = listOf(1, 2, 3).toPersistentList()   // PersistentList: [1, 2, 3]
```

The default [`persistentSetOf`][kotlinx.collections.immutable.persistentSetOf] and
[`persistentMapOf`][kotlinx.collections.immutable.persistentMapOf] preserve element
insertion order during iteration. If iteration order does not matter, the unordered
implementations returned by
[`persistentHashSetOf`][kotlinx.collections.immutable.persistentHashSetOf] and
[`persistentHashMapOf`][kotlinx.collections.immutable.persistentHashMapOf] are more
memory- and time-efficient.

## Modifying persistent collections

The `+` and `-` operators are defined on
[`PersistentCollection`][kotlinx.collections.immutable.PersistentCollection] and
[`PersistentMap`][kotlinx.collections.immutable.PersistentMap]. They delegate to the
collection itself and return a new persistent value. To make them take precedence over the
standard-library operators (which return plain [`List`][kotlin.collections.List],
[`Set`][kotlin.collections.Set], or [`Map`][kotlin.collections.Map]), import the package
explicitly:

```kotlin
import kotlinx.collections.immutable.*

val list = persistentListOf("a", "b") + "c"         // PersistentList<String>
val map = persistentMapOf("x" to 1, "y" to 2) - "x" // PersistentMap<String, Int>
```

When several modifications are applied in sequence,
[`mutate { }`][kotlinx.collections.immutable.mutate] is more efficient than chaining
operators. It exposes a temporary builder, runs the block against it, and returns a fresh
persistent collection at the end:

```kotlin
val result = persistentListOf(1, 2, 3).mutate {
    it.add(4)
    it.removeAt(0)
    it[0] = 20
}
// result: PersistentList<Int> = [20, 3, 4]
```

The builder seen inside [`mutate`][kotlinx.collections.immutable.mutate] implements the
standard [`MutableList`][kotlin.collections.MutableList],
[`MutableSet`][kotlin.collections.MutableSet], or
[`MutableMap`][kotlin.collections.MutableMap] interface, so any code that already works
with mutable collections can be reused inside the block.

## Multiplatform support

`kotlinx-collections-immutable` is a Kotlin Multiplatform library and supports all targets — JVM, JS, Wasm (JS and WASI), and all Kotlin/Native
[targets](https://kotlinlang.org/docs/native-target-support.html).

# Package kotlinx.collections.immutable

The main API: immutable and persistent collection interfaces
([`ImmutableList`][kotlinx.collections.immutable.ImmutableList],
[`PersistentMap`][kotlinx.collections.immutable.PersistentMap], etc.), entry-point factory
functions ([`persistentListOf`][kotlinx.collections.immutable.persistentListOf],
[`persistentSetOf`][kotlinx.collections.immutable.persistentSetOf],
[`persistentMapOf`][kotlinx.collections.immutable.persistentMapOf] and their hash-based
counterparts), and extension functions for conversion
([`toImmutableList`][kotlinx.collections.immutable.toImmutableList],
[`toPersistentSet`][kotlinx.collections.immutable.toPersistentSet], …), operators
(`+`, `-`), and batch updates ([`mutate`][kotlinx.collections.immutable.mutate]).

# Package kotlinx.collections.immutable.adapters

Adapter classes that wrap a standard read-only
[`Collection`][kotlin.collections.Collection], [`List`][kotlin.collections.List],
[`Set`][kotlin.collections.Set], or [`Map`][kotlin.collections.Map] and expose it through
the corresponding `Immutable*` interface by delegation. Useful when an existing collection
needs to fit into an API that expects an immutable type. Note that adapters do not freeze
the underlying collection; the immutability contract holds only if the caller guarantees
the wrapped instance is not mutated elsewhere.

# Package kotlinx.collections.immutable.implementations.immutableList

Internal trie-based vector implementation behind
[`PersistentList`][kotlinx.collections.immutable.PersistentList]. Obtain instances through
[`persistentListOf`][kotlinx.collections.immutable.persistentListOf] or
[`toPersistentList`][kotlinx.collections.immutable.toPersistentList]; concrete types in
this package are an implementation detail and may change between releases.
