/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.immutableList.persistentVectorOf
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import kotlinx.collections.immutable.implementations.persistentOrderedSet.PersistentOrderedSet
import kotlinx.collections.immutable.implementations.persistentOrderedSet.PersistentOrderedSetBuilder

/**
 * Returns a new persistent set with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable set passed to the [mutator] closure has the same contents as this persistent set.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.mutate
 */
public inline fun <T> PersistentSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentSet<T> = builder().apply(mutator).build()

/**
 * Returns a new persistent list with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable list passed to the [mutator] closure has the same contents as this persistent list.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.mutate
 */
public inline fun <T> PersistentList<T>.mutate(mutator: (MutableList<T>) -> Unit): PersistentList<T> = builder().apply(mutator).build()

/**
 * Returns a new persistent map with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable map passed to the [mutator] closure has the same contents as this persistent map.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.mutate
 */
@Suppress("UNCHECKED_CAST")
public inline fun <K, V> PersistentMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).builder().apply(mutator).build()


/**
 * Returns a new persistent collection with the specified [element] added,
 * or this instance if this collection does not support duplicates and it already contains the element.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.plusElement
 */
public inline operator fun <E> PersistentCollection<E>.plus(element: E): PersistentCollection<E> = adding(element)

/**
 * Returns a new persistent collection with a single appearance of the specified [element] removed,
 * or this instance if there is no such element in this collection.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.minusElement
 */
public inline operator fun <E> PersistentCollection<E>.minus(element: E): PersistentCollection<E> = removing(element)


/**
 * Returns a new persistent collection with elements of the specified [elements] collection added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.plusCollection
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) addingAll(elements) else builder().also { it.addAll(elements) }.build()

/**
 * Returns a new persistent collection with elements of the specified [elements] array added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.plusCollection
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()

/**
 * Returns a new persistent collection with elements of the specified [elements] sequence added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.plusCollection
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Sequence<E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()


/**
 * Returns a new persistent collection containing all elements of this collection
 * except the elements contained in the specified [elements] collection,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.minusCollection
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) removingAll(elements) else builder().also { it.removeAll(elements) }.build()

/**
 * Returns a new persistent collection containing all elements of this collection
 * except the elements contained in the specified [elements] array,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.minusCollection
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.removeAll(elements) }.build()

/**
 * Returns a new persistent collection containing all elements of this collection
 * except the elements contained in the specified [elements] sequence,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.minusCollection
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Sequence<E>): PersistentCollection<E>
        =  builder().also { it.removeAll(elements) }.build()


/**
 * Returns a new persistent list with the specified [element] appended.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.plusElement
 */
public inline operator fun <E> PersistentList<E>.plus(element: E): PersistentList<E> = adding(element)

/**
 * Returns a new persistent list with the first appearance of the specified [element] removed,
 * or this instance if there is no such element in this list.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.minusElement
 */
public inline operator fun <E> PersistentList<E>.minus(element: E): PersistentList<E> = removing(element)


/**
 * Returns a new persistent list with elements of the specified [elements] collection appended,
 * or this instance if the specified collection is empty.
 *
 * The elements are appended in the order they appear in the specified collection.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.plusCollection
 */
public operator fun <E> PersistentList<E>.plus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns a new persistent list with elements of the specified [elements] array appended,
 * or this instance if the specified array is empty.
 *
 * The elements are appended in the order they appear in the specified array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.plusCollection
 */
public operator fun <E> PersistentList<E>.plus(elements: Array<out E>): PersistentList<E>
        = mutate { it.addAll(elements) }

/**
 * Returns a new persistent list with elements of the specified [elements] sequence appended,
 * or this instance if the specified sequence is empty.
 *
 * The elements are appended in the order they appear in the specified sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.plusCollection
 */
public operator fun <E> PersistentList<E>.plus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.addAll(elements) }


/**
 * Returns a new persistent list containing all elements of this list
 * except the elements contained in the specified [elements] collection,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.minusCollection
 */
public operator fun <E> PersistentList<E>.minus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns a new persistent list containing all elements of this list
 * except the elements contained in the specified [elements] array,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.minusCollection
 */
public operator fun <E> PersistentList<E>.minus(elements: Array<out E>): PersistentList<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns a new persistent list containing all elements of this list
 * except the elements contained in the specified [elements] sequence,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.minusCollection
 */
public operator fun <E> PersistentList<E>.minus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.removeAll(elements) }


/**
 * Returns a new persistent set with the specified [element] added,
 * or this instance if it already contains the element.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.plusElement
 */
public inline operator fun <E> PersistentSet<E>.plus(element: E): PersistentSet<E> = adding(element)

/**
 * Returns a new persistent set with the specified [element] removed,
 * or this instance if there is no such element in this set.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.minusElement
 */
public inline operator fun <E> PersistentSet<E>.minus(element: E): PersistentSet<E> = removing(element)


/**
 * Returns a new persistent set with elements of the specified [elements] collection added,
 * or this instance if it already contains every element of the specified collection.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.plusCollection
 */
public operator fun <E> PersistentSet<E>.plus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] array added,
 * or this instance if it already contains every element of the specified array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.plusCollection
 */
public operator fun <E> PersistentSet<E>.plus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] sequence added,
 * or this instance if it already contains every element of the specified sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.plusCollection
 */
public operator fun <E> PersistentSet<E>.plus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.addAll(elements) }


/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] collection,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.minusCollection
 */
public operator fun <E> PersistentSet<E>.minus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] array,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.minusCollection
 */
public operator fun <E> PersistentSet<E>.minus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] sequence,
 * or this instance if there are no elements to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.minusCollection
 */
public operator fun <E> PersistentSet<E>.minus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set with elements in this set that are also
 * contained in the specified [elements] collection,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.intersect
 */
public infix fun <E> PersistentSet<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) retainingAll(elements) else mutate { it.retainAll(elements) }

/**
 * Returns a persistent set with elements in this collection that are also
 * contained in the specified [elements] collection.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentCollectionSamples.intersect
 */
public infix fun <E> PersistentCollection<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = this.toPersistentSet().intersect(elements)

/**
 * Returns a new persistent map with an entry from the specified key-value [pair] added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.plusPair
 */
@Suppress("UNCHECKED_CAST")
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pair: Pair<K, V>): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).putting(pair.first, pair.second)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.plusPairs
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.plusPairs
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.plusPairs
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.plusMap
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(map: Map<out K, V>): PersistentMap<K, V> = puttingAll(map)


/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.puttingAll
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> PersistentMap<out K, V>.puttingAll(map: Map<out K, V>): PersistentMap<K, V> =
    (this as PersistentMap<K, V>).puttingAll(map)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases. Refer to the
 * [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(map)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(map: Map<out K, V>): PersistentMap<K, V> = puttingAll(map)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.puttingAllPairs
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.puttingAllPairs
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.puttingAllPairs
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns a new persistent map with the specified [key] and its corresponding value removed,
 * or this instance if it contains no mapping for the key.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.minusKey
 */
@Suppress("UNCHECKED_CAST")
public operator fun <K, V> PersistentMap<out K, V>.minus(key: K): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).removing(key)

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] collection,
 * or this instance if there are no entries to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.minusKeys
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Iterable<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] array,
 * or this instance if there are no entries to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.minusKeys
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Array<out K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] sequence,
 * or this instance if there are no entries to remove.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.minusKeys
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Sequence<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }


/**
 * Returns a new persistent list of the specified elements.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.persistentListOf
 */
public fun <E> persistentListOf(vararg elements: E): PersistentList<E> = persistentVectorOf<E>().addingAll(elements.asList())

/**
 * Returns an empty persistent list.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.persistentListOf
 */
public fun <E> persistentListOf(): PersistentList<E> = persistentVectorOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.persistentSetOf
 */
public fun <E> persistentSetOf(vararg elements: E): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Returns an empty persistent set.
 *
 * Elements of the returned set and sets derived from it are iterated in the order they were added.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.persistentSetOf
 */
public fun <E> persistentSetOf(): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>()


/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.persistentHashSetOf
 */
public fun <E> persistentHashSetOf(vararg elements: E): PersistentSet<E> = PersistentHashSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Returns an empty persistent set.
 *
 * Order of the elements in the returned set and sets derived from it is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.persistentHashSetOf
 */
public fun <E> persistentHashSetOf(): PersistentSet<E> = PersistentHashSet.emptyOf()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.persistentMapOf
 */
public fun <K, V> persistentMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentOrderedMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 *
 * Entries of the returned map and maps derived from it are iterated in the order they were added.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.persistentMapOf
 */
public fun <K, V> persistentMapOf(): PersistentMap<K, V> = PersistentOrderedMap.emptyOf()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.persistentHashMapOf
 */
public fun <K, V> persistentHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentHashMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 *
 * Order of the entries in the returned map and maps derived from it is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.persistentHashMapOf
 */
public fun <K, V> persistentHashMapOf(): PersistentMap<K, V> = PersistentHashMap.emptyOf()


/**
 * Returns a new persistent list of the specified elements.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf(*elements)"))
public fun <E> immutableListOf(vararg elements: E): PersistentList<E> = persistentListOf(*elements)

/**
 * Returns an empty persistent list.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf()"))
public fun <E> immutableListOf(): PersistentList<E> = persistentListOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf(*elements)"))
public fun <E> immutableSetOf(vararg elements: E): PersistentSet<E> = persistentSetOf(*elements)

/**
 * Returns an empty persistent set.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf()"))
public fun <E> immutableSetOf(): PersistentSet<E> = persistentSetOf()


/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 */
@Deprecated("Use persistentHashSetOf instead.", ReplaceWith("persistentHashSetOf(*elements)"))
public fun <E> immutableHashSetOf(vararg elements: E): PersistentSet<E> = persistentHashSetOf(*elements)


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
@Deprecated("Use persistentMapOf instead.", ReplaceWith("persistentMapOf(*pairs)"))
public fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentMapOf(*pairs)

/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 */
@Deprecated("Use persistentHashMapOf instead.", ReplaceWith("persistentHashMapOf(*pairs)"))
public fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)


/**
 * Returns an immutable list containing all elements of this iterable.
 *
 * If the receiver is already an immutable list, returns it as is.
 * If the receiver is a persistent list builder, calls `build` on it and returns the result.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toImmutableList
 */
public fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: this.toPersistentList()

/**
 * Returns an immutable list containing all elements of this array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toImmutableList
 */
public fun <T> Array<out T>.toImmutableList(): ImmutableList<T> = toPersistentList()

/**
 * Returns an immutable list containing all elements of this sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toImmutableList
 */
public fun <T> Sequence<T>.toImmutableList(): ImmutableList<T> = toPersistentList()

/**
 * Returns an immutable list containing all characters.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toImmutableList
 */
public fun CharSequence.toImmutableList(): ImmutableList<Char> = toPersistentList()


/**
 * Returns a persistent list containing all elements of this iterable.
 *
 * If the receiver is already a persistent list, returns it as is.
 * If the receiver is a persistent list builder, calls `build` on it and returns the result.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toPersistentList
 */
public fun <T> Iterable<T>.toPersistentList(): PersistentList<T> =
        this as? PersistentList
        ?: (this as? PersistentList.Builder)?.build()
        ?: persistentListOf<T>() + this

/**
 * Returns a persistent list containing all elements of this array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toPersistentList
 */
public fun <T> Array<out T>.toPersistentList(): PersistentList<T> = persistentListOf<T>() + this

/**
 * Returns a persistent list containing all elements of this sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toPersistentList
 */
public fun <T> Sequence<T>.toPersistentList(): PersistentList<T> = persistentListOf<T>() + this

/**
 * Returns a persistent list containing all characters.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentListSamples.toPersistentList
 */
public fun CharSequence.toPersistentList(): PersistentList<Char> =
    persistentListOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns an immutable set of all elements of this iterable.
 *
 * If the receiver is already an immutable set, returns it as is.
 * If the receiver is a persistent set builder, calls `build` on it and returns the result.
 *
 * Elements of the returned set are iterated in the same order as in this iterable.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toImmutableSet
 */
public fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        this as? ImmutableSet<T>
        ?: (this as? PersistentSet.Builder)?.build()
        ?: persistentSetOf<T>() + this

/**
 * Returns an immutable set of all elements of this array.
 *
 * Elements of the returned set are iterated in the same order as in this array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toImmutableSet
 */
public fun <T> Array<out T>.toImmutableSet(): ImmutableSet<T> = toPersistentSet()

/**
 * Returns an immutable set of all elements of this sequence.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toImmutableSet
 */
public fun <T> Sequence<T>.toImmutableSet(): ImmutableSet<T> = toPersistentSet()

/**
 * Returns an immutable set of all characters.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toImmutableSet
 */
public fun CharSequence.toImmutableSet(): PersistentSet<Char> = toPersistentSet()


/**
 * Returns a persistent set of all elements of this iterable.
 *
 * If the receiver is already an ordered persistent set, returns it as is.
 * If the receiver is an ordered persistent set builder, calls `build` on it and returns the result.
 *
 * Elements of the returned set are iterated in the same order as in this iterable.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentSet
 */
public fun <T> Iterable<T>.toPersistentSet(): PersistentSet<T> =
        this as? PersistentOrderedSet<T>
        ?: (this as? PersistentOrderedSetBuilder)?.build()
        ?: PersistentOrderedSet.emptyOf<T>() + this

/**
 * Returns a persistent set of all elements of this array.
 *
 * Elements of the returned set are iterated in the same order as in this array.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentSet
 */
public fun <T> Array<out T>.toPersistentSet(): PersistentSet<T> = persistentSetOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentSet
 */
public fun <T> Sequence<T>.toPersistentSet(): PersistentSet<T> = persistentSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentSet
 */
public fun CharSequence.toPersistentSet(): PersistentSet<Char> =
        persistentSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns a persistent set containing all elements from this iterable.
 *
 * If the receiver is already a persistent hash set, returns it as is.
 * If the receiver is a persistent hash set builder, calls `build` on it and returns the result.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentHashSet
 */
public fun <T> Iterable<T>.toPersistentHashSet(): PersistentSet<T>
    = this as? PersistentHashSet
        ?: (this as? PersistentHashSetBuilder<T>)?.build()
        ?: PersistentHashSet.emptyOf<T>() + this

/**
 * Returns a persistent set of all elements of this array.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentHashSet
 */
public fun <T> Array<out T>.toPersistentHashSet(): PersistentSet<T> = persistentHashSetOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentHashSet
 */
public fun <T> Sequence<T>.toPersistentHashSet(): PersistentSet<T> = persistentHashSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentSetSamples.toPersistentHashSet
 */
public fun CharSequence.toPersistentHashSet(): PersistentSet<Char> =
        persistentHashSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Returns an immutable map containing all entries from this map.
 *
 * If the receiver is already an immutable map, returns it as is.
 * If the receiver is a persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.toImmutableMap
 */
public fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? PersistentMap.Builder)?.build()
        ?: persistentMapOf<K, V>().puttingAll(this)

/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already an ordered persistent map, returns it as is.
 * If the receiver is an ordered persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.toPersistentMap
 */
public fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentOrderedMap<K, V>
        ?: (this as? PersistentOrderedMapBuilder<K, V>)?.build()
        ?: PersistentOrderedMap.emptyOf<K, V>().puttingAll(this)

/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already a persistent hash map, returns it as is.
 * If the receiver is a persistent hash map builder, calls `build` on it and returns the result.
 *
 * Order of the entries in the returned map is unspecified.
 *
 * @sample kotlinx.collections.immutable.samples.PersistentMapSamples.toPersistentHashMap
 */
public fun <K, V> Map<K, V>.toPersistentHashMap(): PersistentMap<K, V>
        = this as? PersistentHashMap
        ?: (this as? PersistentHashMapBuilder<K, V>)?.build()
        ?: PersistentHashMap.emptyOf<K, V>().puttingAll(this)
