# Immutable Collections

* **Type**: kotlinx library API proposal
* **Authors**: Ilya Gorbunov, Abduqodiri Qurbonzoda 
* **Status**: Submitted
* **Prototype**: In progress
* **Discussion**: In the [issues](https://github.com/Kotlin/kotlinx.collections.immutable/issues) of this repository

## Summary

There are two families of collection interfaces in the Standard Library:

* read-only interfaces, which only allow to read elements from a collection: 
`Collection`, `List`, `Set`, `Map`;
* mutable interfaces, which additionally allow to modify elements in a collection: 
`MutableCollection`, `MutableList`, `MutableSet`, `MutableMap`.

Kotlin mostly doesn't provide its own collection implementations: in Kotlin/JVM
the collection interfaces are mapped to the standard JDK collection interfaces
and implemented by the JDK collection implementations, such as `ArrayList`, `HashSet`, `HashMap` and other
classes from `java.util` package.

It is proposed to provide two families of interfaces that extend read-only collection interfaces:

- Immutable collections: `ImmutableList`, `ImmutableSet`, etc. They specify by their contract the real immutability of their implementors.

- Persistent collections: `PersistentList`, `PersistentSet`, etc. They extend immutable collections and provide efficient
modification operations that return new instances of persistent collections with the modification applied. 
The returned collections can share parts of data structure with the original persistent collections.

For persistent collection interfaces there shall be provided the following implementations:

- default persistent list
- default persistent hash set preserving the insertion order
- persistent hash set
- default persistent hash map preserving the insertion order
- persistent hash map

## Use cases

* Avoiding defensive copying of collection passed as a parameter
  when you need an unchangeable snapshot and you're unsure whether the collection could be changed later.

    ```
    class Query(val parameters: ImmutableList<Parameter>)

    // or

    class Query(parameters: List<Parameter>) {
        // creates an immutable list or does nothing if it's already immutable
        val parameters = parameters.toImmutable()
    }
    ```

* Sharing a single immutable collection between multiple threads. Since the collection
cannot be changed neither by producer, nor by any consumer, you'll never get a `ConcurrentModificationException`
during iteration, or any other symptoms of corrupted state.

* Having multiple snapshots of a collection sharing their element storage.


## Similar API review

* [.NET immutable collections](https://msdn.microsoft.com/en-us/library/mt452182.aspx)
* [Scala immutable collections](https://docs.scala-lang.org/overviews/collections/concrete-immutable-collection-classes.html)


## Alternatives

* Use mutable collections, but once they are populated expose them only though their read-only interfaces.
  This pattern is mostly used in the standard library collection operations.

  * Pro: mutable collection implementations are generally more memory efficient,
  require less allocations during and after their populating.
  * Con: collection can be cast back to mutable (e.g. involving platform types),
  and then unexpectedly mutated.
  * Con: different snapshots of a collection do not share any element storage.
  * Con: defensive copying of the resulting collection is still required later
  in a chain of operations.
  * Con: mutable collections need safe publication to share them to a different thread.



## API details

### Modification operations

Immutable collections just extend the read-only collections. The immutability is enforced solely by their contract:
the implementors are required to keep the collection elements unchanged after the collection is constructed.

Persistent collections provide efficient modification operations on top of that.
These are same operations as their mutable counterparts have, such as `add(element)`,
`remove(element)`, `put(key, value)`, etc., but unlike mutating operations they do not mutate the receiver, but instead
return a new persistent collection instance with the modification applied. The efficiency is achieved by sharing parts 
of collection data structure that are not changed as a result of the operation and can be shared between multiple
collection instances. 

In case no modification is made during the operation, for example, adding an existing element to a set or clearing
an already empty collection, the operation should return the same persistent collection instance.

### Builders

Often there is a need to perform several subsequent modification operations on a persistent collection.
It can be done by the following means:

- chaining operations together

    ```
    collection = collection.add(element).add(anotherElement)
    ```
    
- applying operations one by one

    ```
    collection += element
    collection += anotherElement
    ```
    
- or using collection builders.

A persistent collection builder is a wrapper around the persistent collection underlying data structure, 
that exposes its modification operations through the corresponding mutable collection interface. 
For example, `PersistentSet.Builder<T>` extends `MutableSet<T>`.

A builder can be obtained from a persistent collection with `builder()` method, and then that builder could be
transformed back to a persistent collection with its `build()` method.

```
val builder = persistentList.builder()
builder.removeAll { it.value > treshold }
if (builder.isEmpty()) {
    builder.add(defaultValue)
}
return builder.build()
```

When a mutating operation is invoked on a builder, it doesn't mutate the original persistent collection it was built from,
but instead it gradually builds a new collection with the requested modifications applied. 

In case if all operations invoked on a builder have caused no modifications,
the collection returned by `build()` method is the same collection the builder was obtained from.

Builders have two advantages:
- Allow to utilize existing API taking mutable collections.
- Builders keep their internal data structures mutable and finalize them only when `build()` method is called.

### Variance

It's convenient for immutable and persistent collections to have the same variance as the read-only collections they extend.
However their modification operations and `builder` functions could have covariant type appear in parameter (contravariant) position,
therefore that parameter type must be annotated with `@UnsafeVariance` annotation, and special care must be given when
implementing such methods.


### Immutable collection interfaces

#### ImmutableCollection

- Extends read-only `Collection` interface.

#### ImmutableList

- Extends `ImmutableCollection` and read-only `List` interfaces.
- Overrides `subList` function so that it in turn returns the sublist as an immutable list.

####  ImmutableSet

- Extends `ImmutableCollection` and read-only `Set` interfaces.

#### ImmutableMap

- Extends read-only `Map` interface.
- Overrides properties `keys`, `values`, and `entries`, to return them as immutable collections.

### Persistent collection interfaces

#### PersistentCollection

- Extends `ImmutableCollection` interface. 
- Provides modification operations: `add`, `addAll`, `remove`, `removeAll`, `removeAll { predicate }`, and `clear`.
- The `builder` function returns an instance of the nested `Builder` interface that extends `MutableCollection`.

#### PersistentList

- Extends `ImmutableList` and `PersistentCollection` interfaces.
- Provides modification operations: `add` and `addAll()` with index parameter, `set`, `removeAt`.
- The `builder` function returns an instance of the nested `Builder` interface that extends `MutableList`.

#### PersistentSet

- Extends `ImmutableSet` and `PersistentCollection` interfaces.
- The `builder` function returns an instance of the nested `Builder` interface that extends `MutableSet`.

#### PersistentMap

- Extends `ImmutableMap` interface.
- Provides modification operations: `put`, `putAll`, `remove` by key, `remove` by key and value, and `clear`.
- The `builder` function returns an instance of the nested `Builder` interface that extends `MutableMap`.

### Persistent collection implementations

#### Default persistent list

It's backed by a bit-mapped trie with branching factor of 32.

Time complexity of operations:
- `get(index)`, `set(index, element)` - O(log<sub>32</sub>N), where N is the size of the instance the operations are applied on.
- `add(index, element)`, `removeAt(index)`, `remove(element)` - O(N).
- `addAll(elements)` - O(N).
- `addAll(index, elements)`, `removeAll(elements)`, `removeAll(predicate)` - O(N M), optimizable to O(N+M), where M is the number of elements to be inserted/removed.
- Iterating elements - O(N).

To optimize frequently used `add(element)` and `removeAt(size - 1)` operations rightmost leaf is referenced directly from the persistent list instance.
This allows to avoid path-copying and gives O(1) time complexity for these two operations.

Small persistent lists, with up to 32 elements, are backed by arrays of corresponding size.

#### Persistent unordered hash set

It's backed by a hash array mapped trie (a.k.a HAMT). Every node has up to 32 children or elements.

Time complexity of operations:
- `add(element)`, `remove(element)`, `contains(element)` - O(log<sub>32</sub>N) in general, but strongly depends on hash codes of the stored elements.
- `addAll(elements)`, `removeAll(elements)`, `removeAll(predicate)`, `containsAll(elements)` - O(M log<sub>32</sub>N) in general.
- Iterating elements - O(N).

#### Persistent unordered hash map

It's backed by a compressed hash-array mapped prefix-tree (a.k.a CHAMP). Every node has up to 32 children or entries.

Time complexity of operations:
- `get(key)`, `put(key, value)`, `remove(key)`, `remove(key, value)`, `containsKey(key)` - O(log<sub>32</sub>N) in average, but strongly depends on hash codes of the stored keys.
- `containsValue(value)` - O(N).
- `putAll(map)` - O(M log<sub>32</sub>N), where M is the number of elements added.
- Iterating `keys`, `values`, `entries` - O(N).

#### Default persistent ordered hash set

It's backed by the _persistent unordered hash map_, which maps every element in this set to the previous and next elements in insertion order.

Every operation on this set turns into one or more operations on the backing map, e.g.:
- `add(element)` turns into updating the next reference of the last element (new element becomes the next) and putting new entry with key equal to the specified element.
- `remove(element)` turns into removing the entry with the key equal to the specified element and updating values for the next and previous elements.
- `contains(element)` turns into `containsKey(element)`.

Iterating elements in this set takes O(N log<sub>32</sub>N) time.

#### Default persistent ordered hash map

It is implemented the same way as the _persistent ordered hash set_, 
except that the backing map stores also value beside next and previous keys.

#### Builders

Builders of the _persistent list_, _persistent unordered hash set_, and _persistent unordered hash map_ 
are backed by the same backing data structures as the corresponding persistent collections. 
Thus, `persistentCollection.builder()` takes constant time consisting of passing backing storage to the new builder instance.
But instead of copying every node to be modified, builder makes sure the node has not already been 
copied by marking copies it makes with its unique identifier. Nodes marked by the builder's identifier can be 
modified in-place by that builder.
`builder.build()` also takes constant time, it consists of passing backing storage to the new persistent collection instance 
and updating builder's identifier, as nodes marked by the current identifier are reachable from the built instance and 
cannot by modified in-place any more.

Builders of the _persistent ordered hash set_ and _persistent ordered hash map_ are backed by the builder of the backing map.

Although time complexity of all operations on a builder is the same as in its corresponding persistent collection, 
avoiding memory allocations in modification operations leads to significant performance improvement in practice.

### Extension functions

#### toImmutableList/Set/Map

Converts a read-only or mutable collection to immutable one.
If the receiver is already immutable and has the required type, returns it as is.

    fun Iterable<T>.toImmutableList(): ImmutableList<T>
    fun Iterable<T>.toImmutableSet(): ImmutableSet<T>

#### toPersistentList/Set/Map

Converts a read-only or mutable collection to persistent one.
If the receiver is already persistent and has the required type, returns it as is.
If the receiver is a builder of the required persistent collection type, calls `build` on it and returns the result.

    fun Iterable<T>.toPersistentList(): PersistentList<T>
    fun Iterable<T>.toPersistentSet(): PersistentSet<T>

#### `+` and `-` operators

`plus` and `minus` operators on persistent collections can delegate the implementation to the collections themselves.

#### mutate

A quite common pattern with builders arises: get a builder, apply some mutating operations on it,
transform it back to an immutable collection:

    collection.builder().apply { some_actions_on(this) }.build()

This pattern could be extracted to an extension function.

```
fun <T> PersitentList<T>.mutate(action: (MutableList<T>) -> Unit): PersitentList<T> =
    builder().apply { action(this) }.build()
```

## Dependencies

What are the dependencies of the proposed API:

* interfaces and implementations depend only on a subset of Kotlin Standard Library available on all supported platforms.

## Placement

* Package: `kotlinx.collections.immutable`
* At first the API is to be provided in a separate module `kotlinx-collections-immutable`,
  but later some parts of it can be placed into `kotlin-stdlib`.

## Reference implementation

Reference implementation is given in this repository, see the
[source](https://github.com/kotlin/kotlinx.collections.immutable/tree/master/kotlinx-collections-immutable/src/main/kotlin/kotlinx/collections/immutable).

## Known issues

- [ ] Immutable collections currently aren't `Serializable`.

## Unresolved questions

- [x] Should we distinguish immutable and persistent collection interfaces and implementations?
  - *Resolution*: [the original proposal](https://github.com/Kotlin/kotlinx.collections.immutable/blob/46d2fc3193cf9672373f37f567f2b731c0df9fea/proposal.md) 
  only described the immutable collection interfaces that were to provide both the immutability contract
  and persistent modification operations. The implementation practice has shown that its quite inconvenient 
  to implement the whole persistent collection contract when all that is required from that collection is immutability.
  The examples of such collections are list's sublist and map's keys, values, and entries collections — 
  while they are immutable, they do not support efficient modification operations.
  Thus we decided to leave immutability to immutable collections and provide modification operations in persistent 
  collections extending immutable ones.
  
- [ ] Should we provide sorted maps and sets?

- [ ] Should we expose immutable collection implementations to the public API? Current experience shows
that it's not mandatory.

- [ ] Immutable map provides `keys`, `entries` sets and `values` collection.
Should these collections be also immutable, or just read-only is enough?
Note that their implementations are not persistent and most of their
modification operations require to make a copy of the collection.

- [x] `Map - key` operation: we do not support such operation for read-only maps, and for mutable maps we
can do `MutableMap.keys -= key`. What would be the analogous operation for `ImmutableMap`?
    - *Resolution:* these operators are implemented in the standard library, so it's fine to implement the same for immutable maps. 

- [ ] `mutate` extension: should the action take `MutableList` as a receiver
or as a parameter (`this` or `it` inside lambda)?

- [ ] `persistentMapOf<K, V>().builder()` requires explicit specification of `K` and `V` type arguments
(until the common type inference system is implemented) and is quite lengthy.
Should we provide some shortcut to infer types from expected map type, like `emptyMapBuilder<K, V>`?

## Future advancements

* Provide other immutable collection interfaces such as `ImmutableStack` and `ImmutableQueue`
and their implementations.
* Improve standard library collection operations to exploit benefits of immutability of the collections on which the operation is performed.

