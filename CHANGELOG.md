# CHANGELOG

## 0.3.6

- Upgrade Kotlin version up to 1.9.0
- Support all targets currently supported by the K/N compiler
- Drop support for the Legacy js target

## 0.3.5

- Upgrade Kotlin version up to 1.6.0
- Raise the JVM bytecode target to 1.8. Now the library cannot be used on JDK 1.6 and 1.7.
- Add other Apple K/N targets
- Implement faster equals function for sets and maps
- Fix PersistentHashMapBuilder.putAll() #[114](https://github.com/Kotlin/kotlinx.collections.immutable/issues/114)

## 0.3.4

- Upgrade Kotlin version up to 1.4.30
- Publish the library to Maven Central instead of Bintray #[96](https://github.com/Kotlin/kotlinx.collections.immutable/issues/96).
- Add license information to published POMs [#98](https://github.com/Kotlin/kotlinx.collections.immutable/issues/98).
- Implement workaround for specialized MutableEntrySet.contains/remove [KT-41278](https://youtrack.jetbrains.com/issue/KT-41278).
- Bug in PersistentList - list is broken after removeAll call [#92](https://github.com/Kotlin/kotlinx.collections.immutable/issues/92).
- Faster bulk operations for non-ordered sets (removeAll, retainAll, containsAll) [#91](https://github.com/Kotlin/kotlinx.collections.immutable/issues/91).
- Faster addAll/putAll implementation for non-ordered sets/maps [#83](https://github.com/Kotlin/kotlinx.collections.immutable/issues/83).
- Add extension functions to convert Sequence<T> to persistent collections [84](https://github.com/Kotlin/kotlinx.collections.immutable/issues/84).
- Add missing CharSequence.toImmutableSet() extension function.

## 0.3.3

- Upgrade Kotlin version up to 1.4.0
- Weaken receiver type of toPersistentHashSet to Iterable [#77](https://github.com/Kotlin/kotlinx.collections.immutable/issues/77).
- Throw ConcurrentModificationException if hashCode of ordered set element changes [#76](https://github.com/Kotlin/kotlinx.collections.immutable/issues/76).
- Fix transition from PersistentVector to SmallPersistentVector [#75](https://github.com/Kotlin/kotlinx.collections.immutable/issues/75)
- Add CharSequence.toPersistentHashSet() as an alternative to CharSequence.toPersistentSet()

## 0.3.2

- Introduce `persistentHashSetOf`, `persistentMapOf` and `persistentHashMapOf` methods that take no arguments and create an empty instance [#67](https://github.com/Kotlin/kotlinx.collections.immutable/issues/67).
- Fix map `entries`/`keys`/`values` iterators including [#68](https://github.com/Kotlin/kotlinx.collections.immutable/issues/68).

## 0.3.1

- Update Kotlin version up to 1.3.70

## 0.3

- Turn the JVM-only project into a multiplatform library
    * Builder iterators are fast-fail only on JVM. On the other platforms modifying the builder during iteration not through the corresponding iterator can invalidate the iterator state in an unspecified way.
    * In addition to JVM and JS platforms, macosX64, iosX64, iosArm64, iosArm32, linuxX64, and mingwX64 native platforms are supported.
- Make conversion to persistent collections consistent
    * `toPersistentMap`/`Set` always returns an ordered persistent map/set
    * `toPersistentHashMap`/`Set` always returns an unordered persistent map/set
    * `toImmutableMap`/`Set` may return any kind of immutable map/set
- Optimize persistent list [builder] batch update operations
    * `addAll(elements)` operation performs ~3 times faster :chart_with_downwards_trend:
    * `addAll(index, elements)` operation takes O(N + M), down from O(N * M), where N is the size of this collection and M - the size of the `elements` collection. :chart_with_downwards_trend:
    * `removeAll(elements)` operation takes O(N * K), down from O(N * M), where K is the time complexity of `contains` operation on the `elements` collection :chart_with_downwards_trend:
    * `removeAll(predicate)` operation takes O(N * P), down from O(N * (P + N)), where P is the time complexity of the `predicate` algorithm :chart_with_downwards_trend:
- Implement set/map backing trie canonicalization
    * `add` operation after `remove` operations performs ~20% faster :chart_with_downwards_trend:
    * iteration after `remove` operations performs ~3 times faster :chart_with_downwards_trend:

## 0.2

#### Split immutable and persistent interfaces

- Immutable collections specify by their contract the real immutability of their implementors
    * `ImmutableCollection` extends read-only `Collection`
    * `ImmutableList` extends read-only `List` and `ImmutableCollection`, and overrides `subList` method that returns `ImmutableList`
    * `ImmutableSet` extends read-only `Set` and `ImmutableCollection`
    * `ImmutableMap` extends read-only `Map`, and overrides `keys`, `values` and `entries` properties types with `ImmutableSet`, `ImmutableCollection` and `ImmutableSet` respectively
- Persistent collections extend immutable collections and provide modification operations that return new instances with the modification applied 
    * `PersistentCollection` extends `ImmutableCollection`, and provides modification operations and builder
    * `PersistentList` extends `ImmutableList` and `PersistentCollection`, and provides modification operations
    * `PersistentSet` extends `ImmutableSet` and `PersistentCollection`, and provides modification operations
    * `PersistentMap` extends `ImmutableMap`, and provides modification operations and builder
- `plus`, `minus` and `mutate` extension functions are available only for persistent collections
- Deprecate `immutableXOf()` top-level functions and introduce `persistentXOf()` 
- `toImmutableX()` extension functions return `ImmutableX`
- Introduce `toPersistentX()` extension functions that return `PersistentX`
- Document public API

#### Replace PCollection-based prototypes with custom performant implementations

 - `PersistentList` implementation is backed by a bit-mapped trie with branching factor of 32
    * `add(element: E)` and `removeAt(size - 1)` operations take O(1) time, down from O(log<sub>2</sub>n) :chart_with_downwards_trend:
    * `get` and `set` operations take O(log<sub>32</sub>n), down from O(log<sub>2</sub>n) (though the same asymptotic) :chart_with_downwards_trend:
    * Iteration has the same time complexity of O(n), but much faster in practice due to the better reference locality :chart_with_downwards_trend:
 - Unordered `PersistentSet` implementation is backed by a hash-array mapped trie (a.k.a. HAMT) with up to 32 children or elements in a node
    * `contains`, `add` and `remove` operations take O(log<sub>32</sub>n) time, down from O(log<sub>2</sub>n) :chart_with_downwards_trend:
    * Iteration has the same time complexity of O(n), but much faster in practice due to the better reference locality :chart_with_downwards_trend:
 - Unordered `PersistentMap` implementation is backed by a compressed hash-array mapped prefix-tree (a.k.a. CHAMP) with up to 32 children or entries in a node
    * `contains`, `get`, `put` and `remove` operations take O(log<sub>32</sub>n) time, down from O(log<sub>2</sub>n) :chart_with_downwards_trend:
    * Iteration has the same time complexity of O(n), but much faster in practice due to the better reference locality :chart_with_downwards_trend:
 - Ordered `PersistentSet` implementation is backed by the unordered `PersistentMap` which maps elements in this set to next and previous elements in insertion order
    * `contains`, `get` and `put` operations take O(log<sub>32</sub>n) time, down from O(log<sub>2</sub>n) :chart_with_downwards_trend:
    * `remove` operation takes O(log<sub>32</sub>n) time, down from O(n) :chart_with_downwards_trend:
    * Iteration takes O(n log<sub>32</sub>n) time, up from O(n) :chart_with_upwards_trend:
 - Ordered `PersistentMap` implementation is backed by the unordered `PersistentMap` which maps keys in this map to values, next and previous keys in insertion order
    * `contains`, `get` and `put` operations take O(log<sub>32</sub>n) time, down from O(log<sub>2</sub>n) :chart_with_downwards_trend:
    * `remove` operation takes O(log<sub>32</sub>n) time, down from O(n) :chart_with_downwards_trend:
    * Iteration takes O(n log<sub>32</sub>n) time, up from O(n) :chart_with_upwards_trend:
 - Builders are backed by the same backing storage as the corresponding persistent collections, but apply modifications in-place if the node has already been copied
    * Time complexities of all operations are the same as of the corresponding persistent collections. However, avoiding memory allocations leads to significant performance improvement :chart_with_downwards_trend:
