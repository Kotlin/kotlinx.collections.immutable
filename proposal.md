# Immutable Collections

* **Type**: Standard/kotlinx Library API proposal
* **Author**: Ilya Gorbunov
* **Status**: Submitted
* **Prototype**: In progress
* **Discussion**: In the [issues](https://github.com/Kotlin/kotlinx.collections.immutable/issues) of this repository

## Summary

Provide interfaces to represent truly immutable collections and maps.

Provide implementation of those interfaces.

## Similar API review

* [.NET immutable collections](https://msdn.microsoft.com/en-us/library/mt452182.aspx)
* [Scala immutable collections](http://docs.scala-lang.org/overviews/collections/concrete-immutable-collection-classes)

## Description

There are two families of collection interfaces in the Standard Library:

* read-only interfaces, which only allow to read elements from a collection;
* mutable interfaces, which additionally allow to modify elements in a collection.

Kotlin mostly doesn't provide its own collection implementations.
Its collection interfaces are mapped to the standard JDK collection interfaces
and implemented by the JDK collection implementations, such as `ArrayList`, `HashSet`, `HashMap` and other
classes from `java.util` package.

It is proposed to provide immutable collection interfaces which extend read-only collection interfaces
and specify by the contract the real immutability of their implementors.

### Modification operations

Immutable collections could have same modification operations as their mutable counterparts, such as `add(element)`,
`remove(element)`, `put(key, value)` etc., but unlike mutating operations they do not mutate the receiver, but instead
return new immutable collection instance with the modification applied.

In case if no modification is made during the operation, for example, adding an existing element to a set or clearing
already empty collection, the operation should return the same instance of the immutable collection.

### Builders

Often there is a need to perform several subsequent modification operations on an immutable collection. It can be done by
- chaining operations together:

    ```
    collection = collection.add(element).add(anotherElement)
    ```
    
- applying operations one by one:

    ```
    collection += element
    collection += anotherElement
    ```
    
- using collection builders.

An immutable collection builder is a wrapper around an immutable collection, which exposes its modification operations
within the mutable collection interface. For example, `ImmutableSet.Builder<T>` extends `MutableSet<T>`.

An immutable collection could be transformed to a builder with `builder()` method, and a builder could be
transformed back to the immutable collection with its `build()` method.

```
val builder = immutableList.builder()
builder.removeAll { it.value > treshold }
if (builder.isEmpty()) {
    builder.add(defaultValue)
}
return builder.build()
```

When mutating operation is invoked on a builder, it doesn't mutate the original immutable collection it was built from,
but instead it gradually builds new immutable collection with the requested modifications applied. 

In case if each operation invoked on a builder causes no modifications,
the collection returned by `build()` method is the same collection the builder was obtained from.

Builders have two advantages:
- Allow to utilize existing API taking mutable collections.
- Builders could keep their internal data structures mutable and finalize them only when `build()` method is called.

### Variance

It's convenient for immutable collections to have the same variance as the read-only collections they extend.
However their modification operations could have covariant type appear in parameter (contravariant) position,
therefore that parameter type must be annotated with `@UnsafeVariance` annotation, and special care must be given when
implementing such methods.

### Interfaces and implementations

#### ImmutableCollection

- Extends read-only Collection.
- Provides builder as a mutable collection.

#### ImmutableList

- Extends read-only List and immutable Collection.
- Provides builder as a mutable list.
- Has the single implementation, returned by `immutableListOf` function.

#### ImmutableSet

- Extends read-only Set and immutable Collection.
- Provides builder as a mutable set.
- Has two implementations (`immutableSetOf`, `immutableHashSetOf`)
differing by whether the insertion order of elements is preserved.

#### ImmutableMap

- Extends read-only Map.
- Provides builder as a mutable map.
- Has two implementations (`immutableMapOf`, `immutableHashMapOf`)
differing by whether the insertion order of keys is preserved.

### Extension functions

#### toImmutableList/Set/Map
Converts read-only or mutable collection to immutable one.
If the receiver is already immutable and has the required type, returns itself.

    fun Iterable<T>.toImmutableList(): ImmutableList<T>
    fun Iterable<T>.toImmutableSet(): ImmutableSet<T>

#### `+` and `-` operators

`plus` and `minus` operators on immutable collections can exploit their immutability
and delegate the implementation to the collections themselves.

#### mutate

A quite common pattern with builders arises: get a builder, apply some mutating operations on it,
transform it back to an immutable collection:

    collection.builder().apply { some_actions_on(this) }.build()

This pattern could be extracted to an extension function.

```
ImmutableList.mutate(action: (MutableList) -> Unit) =
    builder().apply { action(this) }.build()
```

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

## Alternatives

* Create a mutable collection, but expose it only as its read-only interface.
  This pattern is mostly employed in the standard library collection operations.

  * Pro: mutable collection implementations are generally more memory efficient,
  require less allocations during and after their populating.
  * Con: collection can be cast back to mutable (e.g. involving platform types),
  and then unexpectedly mutated.
  * Con: different snapshots of a collection do not share any element storage.
  * Con: defensive copying of the resulting collection is still required later
  in a chain of operations.


## Dependencies

What are the dependencies of the proposed API:

* interfaces depend only on a subset of Kotlin Standard Library available on all supported platforms.
* current collection implementations are based on [PCollections](http://pcollections.org) library,
  which is being shaded into the resulting artifact. This results in dependency on JVM 1.6 and upper.

## Placement

* Package: `kotlin.collections.immutable`
* At first the API is to be provided in a separate module `kotlin-collections-immutable`,
  but later it may be merged into `kotlin-stdlib`.

## Reference implementation

Reference implementation is given in this repository, see the
[source](https://github.com/ilya-g/kotlinx.collections.immutable/tree/master/kotlinx-collections-immutable/src/main/kotlin/kotlinx/collections/immutable).

## Known issues

1. There is no implementation of immutable ordered map.
2. Immutable collections currently aren't `Serializable`.

## Unresolved questions

1. Should we distinguish immutable and persistent collection interfaces and implementations?
2. Should we provide sorted maps and sets?
3. Should we expose immutable collection implementations to the public API? Current experience shows
that it's not mandatory.
4. Immutable map provides `keys`, `entries` sets and `values` collection.
Should these collections be also immutable, or just read-only is enough?
Note that their implementations are not persistent and most of their
modification operations require to make a copy of the collection.
5. `Map - key` operation: we do not support such operation for read-only maps, and for mutable maps we
can do `MutableMap.keys -= key`. What would be the analogous operation for `ImmutableMap`?
    - *Resolution:* these operators are implemented in the standard library, so it's fine to implement the same for immutable maps. 
6. `mutate` extension: should the action take `MutableList` as a receiver
or as a parameter (`this` or `it` inside lambda)?
7. `immutableMapOf<K,V>().builder()` requires explicit specification of K and V type arguments
(until the common type inference system is implemented) and is quite lengthy.
Should we provide some shortcut to infer types from expected map type?

## Future advancements

* Provide other immutable collection interfaces such as `ImmutableStack` and `ImmutableQueue`
and their implementations.
* Improve standard library collection operations to exploit benefits of immutability of the source collections.

