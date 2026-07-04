# Coverage map

67 sample functions, 108 `@sample` tags. All references use the prefix `kotlinx.collections.immutable.samples`. Overload clusters (the `Iterable`/`Array`/`Sequence` variants of one operation) share one sample.

## extensions.kt (75 tags)

| Declarations | Sample |
|---|---|
| `persistentListOf(vararg)`, `persistentListOf()` | `PersistentListSamples.persistentListOf` |
| `persistentSetOf(vararg)`, `persistentSetOf()` | `PersistentSetSamples.persistentSetOf` |
| `persistentHashSetOf(vararg)`, `persistentHashSetOf()` | `PersistentSetSamples.persistentHashSetOf` |
| `persistentMapOf(vararg)`, `persistentMapOf()` | `PersistentMapSamples.persistentMapOf` |
| `persistentHashMapOf(vararg)`, `persistentHashMapOf()` | `PersistentMapSamples.persistentHashMapOf` |
| `PersistentCollection.plus/minus(element)` | `PersistentCollectionSamples.plusElement` / `.minusElement` |
| `PersistentCollection.plus/minus(Iterable\|Array\|Sequence)` | `PersistentCollectionSamples.plusCollection` / `.minusCollection` |
| `PersistentCollection.intersect(Iterable)` | `PersistentCollectionSamples.intersect` |
| `PersistentList.plus/minus(element)` | `PersistentListSamples.plusElement` / `.minusElement` |
| `PersistentList.plus/minus(Iterable\|Array\|Sequence)` | `PersistentListSamples.plusCollection` / `.minusCollection` |
| `PersistentSet.plus/minus(element)` | `PersistentSetSamples.plusElement` / `.minusElement` |
| `PersistentSet.plus/minus(Iterable\|Array\|Sequence)` | `PersistentSetSamples.plusCollection` / `.minusCollection` |
| `PersistentSet.intersect(Iterable)` | `PersistentSetSamples.intersect` |
| `PersistentList/Set/Map.mutate` | `Persistent{List,Set,Map}Samples.mutate` |
| `PersistentMap.plus(Pair)` | `PersistentMapSamples.plusPair` |
| `PersistentMap.plus(Iterable\|Array\|Sequence of Pair)` | `PersistentMapSamples.plusPairs` |
| `PersistentMap.plus(Map)` | `PersistentMapSamples.plusMap` |
| `PersistentMap.puttingAll(Map)` | `PersistentMapSamples.puttingAll` |
| `PersistentMap.puttingAll(Iterable\|Array\|Sequence of Pair)` | `PersistentMapSamples.puttingAllPairs` |
| `PersistentMap.minus(key)` | `PersistentMapSamples.minusKey` |
| `PersistentMap.minus(Iterable\|Array\|Sequence of keys)` | `PersistentMapSamples.minusKeys` |
| `Iterable/Array/Sequence/CharSequence.toImmutableList` | `PersistentListSamples.toImmutableList` |
| `Iterable/Array/Sequence/CharSequence.toPersistentList` | `PersistentListSamples.toPersistentList` |
| `Iterable/Array/Sequence/CharSequence.toImmutableSet` | `PersistentSetSamples.toImmutableSet` |
| `Iterable/Array/Sequence/CharSequence.toPersistentSet` | `PersistentSetSamples.toPersistentSet` |
| `Iterable/Array/Sequence/CharSequence.toPersistentHashSet` | `PersistentSetSamples.toPersistentHashSet` |
| `Map.toImmutableMap` / `.toPersistentMap` / `.toPersistentHashMap` | `PersistentMapSamples.toImmutableMap` / `.toPersistentMap` / `.toPersistentHashMap` |

## Interface files (33 tags)

| File | Tagged declarations | Samples |
|---|---|---|
| `ImmutableCollection.kt` (4) | `ImmutableCollection`, `PersistentCollection` class docs; `builder()`; `Builder.build()` | `PersistentCollectionSamples.immutableCollection`, `.persistentCollection`, `.builder` (shared by both builder members) |
| `ImmutableList.kt` (13) | `ImmutableList` class, `subList`; `PersistentList` class, `adding`, `addingAll`, `removing`, `removingAll(elements)`, `removingAll(predicate)`, `retainingAll`, `addingAt`, `addingAllAt`, `replacingAt`, `removingAt` | same-named functions in `PersistentListSamples` (`removingAll` pair maps to `removingAllElements`/`removingAllPredicate`) |
| `ImmutableSet.kt` (8) | `ImmutableSet`, `PersistentSet` class docs; `adding`, `addingAll`, `removing`, `removingAll(elements)`, `removingAll(predicate)`, `retainingAll` | same-named functions in `PersistentSetSamples` |
| `ImmutableMap.kt` (8) | `ImmutableMap`, `PersistentMap` class docs; `putting`, `removing(key)`, `removing(key, value)`, `puttingAll(m)`, `builder()`, `Builder.build()` | `PersistentMapSamples.immutableMap`, `.persistentMap`, `.putting`, `.removingKey`, `.removingKeyValue`, `.puttingAll` (shared with the extension), `.builder` (shared by both builder members) |

Untagged by design: deprecated declarations (hidden by `skipDeprecated`), `cleared()` (trivial), the `Builder` interface class docs, and the bare `builder()` overrides in `PersistentList`/`PersistentSet` (no own KDoc; a tag-only KDoc block would suppress the docs inherited from `PersistentCollection.builder()`).

## Doc fixes applied

1. `Iterable.toPersistentSet()`: claimed to return any persistent set "as is"; the implementation short-circuits only `PersistentOrderedSet` (a persistent hash set is copied). Now says "ordered persistent set" / "ordered persistent set builder".
2. `Map.toPersistentMap()`: same fix with `PersistentOrderedMap`.
3. `Iterable.toImmutableList()`, `Iterable.toImmutableSet()`, `Map.toImmutableMap()`: documented the builder-unwrapping branch (`build()` is called on a persistent builder receiver) that the implementations always had.
4. Empty `persistentHashSetOf()` / `persistentHashMapOf()`: documented that the returned collection and collections derived from it have unspecified iteration order — previously indistinguishable from the ordered `persistentSetOf()` / `persistentMapOf()` overloads.
5. Removed a stale commented-out `mutate` prototype above `PersistentSet.mutate` in `extensions.kt`.

Also new: class KDoc for `ImmutableCollectionAdapter`, `ImmutableListAdapter`, `ImmutableSetAdapter`, `ImmutableMapAdapter` (no-copy wrapping, full delegation, caller guarantees the underlying collection never changes).

## Known quirks left as is (ABI-frozen or out of scope)

- `CharSequence.toImmutableSet()` returns `PersistentSet<Char>`, unlike the other three `toImmutableSet` overloads which return `ImmutableSet<T>`. Changing the return type would break ABI.
- `Iterable<T>.toImmutableSet()` promises iteration "in the same order as in this iterable", which cannot hold for the receiver-returned-as-is path when the receiver is an unordered `ImmutableSet`; the sample uses a `listOf` receiver where the promise holds.
- `@throws` on `addingAt`/`addingAllAt` says the index must not be "out of bounds of this list", although `index == size` is a valid insertion position (matches `java.util.List` phrasing).
- `ImmutableCollection`'s class doc says implementors "must contain the same elements in the same order", which reads oddly for unordered sets; per-instance iteration order is in fact fixed, so the sentence is defensible.
