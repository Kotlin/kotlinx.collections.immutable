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

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
/**
 * Returns the result of applying the provided modifications on this set.
 *
 * The mutable set passed to the [mutator] closure has the same contents as this persistent set.
 *
 * @return a new persistent set with the provided modifications applied;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline fun <T> PersistentSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentSet<T> = builder().apply(mutator).build()

/**
 * Returns the result of applying the provided modifications on this list.
 *
 * The mutable list passed to the [mutator] closure has the same contents as this persistent list.
 *
 * @return a new persistent list with the provided modifications applied;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline fun <T> PersistentList<T>.mutate(mutator: (MutableList<T>) -> Unit): PersistentList<T> = builder().apply(mutator).build()

/**
 * Returns the result of applying the provided modifications on this map.
 *
 * The mutable map passed to the [mutator] closure has the same contents as this persistent map.
 *
 * @return a new persistent map with the provided modifications applied;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <K, V> PersistentMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).builder().apply(mutator).build()


/**
 * Returns the result of adding the specified [element] to this collection.
 *
 * @return a new persistent collection with the specified [element] added;
 *         or this instance if this collection does not support duplicates,
 *         and it already contains the element.
 */
public inline operator fun <E> PersistentCollection<E>.plus(element: E): PersistentCollection<E> = adding(element)

/**
 * Returns the result of removing a single appearance of the specified [element] from this collection.
 *
 * @return a new persistent collection with a single appearance of the specified [element] removed;
 *         or this instance if there is no such element in this collection.
 */
public inline operator fun <E> PersistentCollection<E>.minus(element: E): PersistentCollection<E> = removing(element)


/**
 * Returns the result of adding all elements of the specified [elements] collection to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] collection added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) addingAll(elements) else builder().also { it.addAll(elements) }.build()

/**
 * Returns the result of adding all elements of the specified [elements] array to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] array added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()

/**
 * Returns the result of adding all elements of the specified [elements] sequence to this collection.
 *
 * @return a new persistent collection with elements of the specified [elements] sequence added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.plus(elements: Sequence<E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()


/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent collection with elements in this collection that are also
 *         contained in the specified [elements] collection removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) removingAll(elements) else builder().also { it.removeAll(elements) }.build()

/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent collection with elements in this collection that are also
 *         contained in the specified [elements] array removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.removeAll(elements) }.build()

/**
 * Returns the result of removing all elements in this collection that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent collection with elements in this collection that are also
 *         contained in the specified [elements] sequence removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentCollection<E>.minus(elements: Sequence<E>): PersistentCollection<E>
        =  builder().also { it.removeAll(elements) }.build()


/**
 * Returns the result of appending the specified [element] to this list.
 *
 * @return a new persistent list with the specified [element] appended.
 */
public inline operator fun <E> PersistentList<E>.plus(element: E): PersistentList<E> = adding(element)

/**
 * Returns the result of removing the first appearance of the specified [element] from this list.
 *
 * @return a new persistent list with the first appearance of the specified [element] removed;
 *         or this instance if there is no such element in this list.
 */
public inline operator fun <E> PersistentList<E>.minus(element: E): PersistentList<E> = removing(element)


/**
 * Returns the result of appending all elements of the specified [elements] collection to this list.
 *
 * The elements are appended in the order they appear in the specified collection.
 *
 * @return a new persistent list with elements of the specified [elements] collection appended;
 *         or this instance if the specified collection is empty.
 */
public operator fun <E> PersistentList<E>.plus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns the result of appending all elements of the specified [elements] array to this list.
 *
 * The elements are appended in the order they appear in the specified array.
 *
 * @return a new persistent list with elements of the specified [elements] array appended;
 *         or this instance if the specified array is empty.
 */
public operator fun <E> PersistentList<E>.plus(elements: Array<out E>): PersistentList<E>
        = mutate { it.addAll(elements) }

/**
 * Returns the result of appending all elements of the specified [elements] sequence to this list.
 *
 * The elements are appended in the order they appear in the specified sequence.
 *
 * @return a new persistent list with elements of the specified [elements] sequence appended;
 *         or this instance if the specified sequence is empty.
 */
public operator fun <E> PersistentList<E>.plus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.addAll(elements) }


/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent list with elements in this list that are also
 *         contained in the specified [elements] collection removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentList<E>.minus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent list with elements in this list that are also
 *         contained in the specified [elements] array removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentList<E>.minus(elements: Array<out E>): PersistentList<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this list that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent list with elements in this list that are also
 *         contained in the specified [elements] sequence removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentList<E>.minus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.removeAll(elements) }


/**
 * Returns the result of adding the specified [element] to this set.
 *
 * @return a new persistent set with the specified [element] added;
 *         or this instance if it already contains the element.
 */
public inline operator fun <E> PersistentSet<E>.plus(element: E): PersistentSet<E> = adding(element)

/**
 * Returns the result of removing the specified [element] from this set.
 *
 * @return a new persistent set with the specified [element] removed;
 *         or this instance if there is no such element in this set.
 */
public inline operator fun <E> PersistentSet<E>.minus(element: E): PersistentSet<E> = removing(element)


/**
 * Returns the result of adding all elements of the specified [elements] collection to this set.
 *
 * @return a new persistent set with elements of the specified [elements] collection added;
 *         or this instance if it already contains every element of the specified collection.
 */
public operator fun <E> PersistentSet<E>.plus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns the result of adding all elements of the specified [elements] array to this set.
 *
 * @return a new persistent set with elements of the specified [elements] array added;
 *         or this instance if it already contains every element of the specified array.
 */
public operator fun <E> PersistentSet<E>.plus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.addAll(elements) }

/**
 * Returns the result of adding all elements of the specified [elements] sequence to this set.
 *
 * @return a new persistent set with elements of the specified [elements] sequence added;
 *         or this instance if it already contains every element of the specified sequence.
 */
public operator fun <E> PersistentSet<E>.plus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.addAll(elements) }


/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this set that are also
 *         contained in the specified [elements] collection removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentSet<E>.minus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] array.
 *
 * @return a new persistent set with elements in this set that are also
 *         contained in the specified [elements] array removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentSet<E>.minus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns the result of removing all elements in this set that are also
 * contained in the specified [elements] sequence.
 *
 * @return a new persistent set with elements in this set that are also
 *         contained in the specified [elements] sequence removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <E> PersistentSet<E>.minus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }

/**
 * Returns all elements in this set that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this set that are also
 *         contained in the specified [elements] collection;
 *         or this instance if no modifications were made in the result of this operation.
 */
public infix fun <E> PersistentSet<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) retainingAll(elements) else mutate { it.retainAll(elements) }

/**
 * Returns all elements in this collection that are also
 * contained in the specified [elements] collection.
 *
 * @return a new persistent set with elements in this collection that are also
 *         contained in the specified [elements] collection.
 */
public infix fun <E> PersistentCollection<E>.intersect(elements: Iterable<E>): PersistentSet<E>
        = this.toPersistentSet().intersect(elements)

/**
 * Returns the result of adding an entry to this map from the specified key-value [pair].
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 *
 * @return a new persistent map with an entry from the specified key-value [pair] added;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pair: Pair<K, V>): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).putting(pair.first, pair.second)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 *         or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentMap<out K, V>.plus(map: Map<out K, V>): PersistentMap<K, V> = puttingAll(map)


/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> PersistentMap<out K, V>.puttingAll(map: Map<out K, V>): PersistentMap<K, V> =
    (this as PersistentMap<K, V>).puttingAll(map)

/**
 * Returns the result of merging the specified [map] with this map.
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
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(map)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(map: Map<out K, V>): PersistentMap<K, V> = puttingAll(map)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.puttingAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * Use the function [puttingAll] to make it clear that a new map is returned.
 *
 * Old functions mimicking [MutableMap] names, like this one,
 * were deprecated and will be removed in future releases.
 * Refer to the [Migration guide](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/docs/0.5.0-MIGRATION.md)
 * for more details and guidance with the migration.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 *         or this instance if no modifications were made in the result of this operation.
 */
@Deprecated(
    "Use puttingAll() instead. For more details, read the documentation for this function.",
    replaceWith = ReplaceWith("puttingAll(pairs)")
)
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = puttingAll(pairs)

/**
 * Returns the result of removing the specified [key] and its corresponding value from this map.
 *
 * @return a new persistent map with the specified [key] and its corresponding value removed;
 *         or this instance if it contains no mapping for the key.
 */
@Suppress("UNCHECKED_CAST")
public operator fun <K, V> PersistentMap<out K, V>.minus(key: K): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).removing(key)

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Iterable<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Array<out K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 *         or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Sequence<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }


/**
 * Creates a new persistent list of the specified elements.
 *
 * @return a new persistent list of the specified elements.
 */
public fun <E> persistentListOf(vararg elements: E): PersistentList<E> = persistentVectorOf<E>().addingAll(elements.asList())

/**
 * Creates an empty persistent list.
 *
 * @return an empty persistent list.
 */
public fun <E> persistentListOf(): PersistentList<E> = persistentVectorOf()


/**
 * Creates a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 *
 * @return a new persistent set with the given elements.
 */
public fun <E> persistentSetOf(vararg elements: E): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Creates an empty persistent set.
 *
 * @return an empty persistent set.
 */
public fun <E> persistentSetOf(): PersistentSet<E> = PersistentOrderedSet.emptyOf<E>()


/**
 * Creates a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a new persistent set with the given elements.
 */
public fun <E> persistentHashSetOf(vararg elements: E): PersistentSet<E> = PersistentHashSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Creates an empty persistent set.
 *
 * @return an empty persistent set.
 */
public fun <E> persistentHashSetOf(): PersistentSet<E> = PersistentHashSet.emptyOf()


/**
 * Creates a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * @return a new persistent map with the specified contents.
 */
public fun <K, V> persistentMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentOrderedMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Creates an empty persistent map.
 *
 * @return an empty persistent map.
 */
public fun <K, V> persistentMapOf(): PersistentMap<K, V> = PersistentOrderedMap.emptyOf()


/**
 * Creates a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 *
 * @return a new persistent map with the specified contents.
 */
public fun <K, V> persistentHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentHashMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Creates an empty persistent map.
 *
 * @return an empty persistent map.
 */
public fun <K, V> persistentHashMapOf(): PersistentMap<K, V> = PersistentHashMap.emptyOf()


/**
 * Creates a new persistent list of the specified elements.
 *
 * @return a new persistent list of the specified elements.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf(*elements)"))
public fun <E> immutableListOf(vararg elements: E): PersistentList<E> = persistentListOf(*elements)

/**
 * Creates an empty persistent list.
 *
 * @return an empty persistent list.
 */
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf()"))
public fun <E> immutableListOf(): PersistentList<E> = persistentListOf()


/**
 * Creates a new persistent set with the given elements.
 *
 * Elements of the returned set are iterated in the order they were specified.
 *
 * @return a new persistent set with the given elements.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf(*elements)"))
public fun <E> immutableSetOf(vararg elements: E): PersistentSet<E> = persistentSetOf(*elements)

/**
 * Creates an empty persistent set.
 *
 * @return an empty persistent set.
 */
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf()"))
public fun <E> immutableSetOf(): PersistentSet<E> = persistentSetOf()


/**
 * Creates a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a new persistent set with the given elements.
 */
@Deprecated("Use persistentHashSetOf instead.", ReplaceWith("persistentHashSetOf(*elements)"))
public fun <E> immutableHashSetOf(vararg elements: E): PersistentSet<E> = persistentHashSetOf(*elements)


/**
 * Creates a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 *
 * @return a new persistent map with the specified contents.
 */
@Deprecated("Use persistentMapOf instead.", ReplaceWith("persistentMapOf(*pairs)"))
public fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentMapOf(*pairs)

/**
 * Creates a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 *
 * @return a new persistent map with the specified contents.
 */
@Deprecated("Use persistentHashMapOf instead.", ReplaceWith("persistentHashMapOf(*pairs)"))
public fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)


/**
 * Converts this collection to an immutable list.
 *
 * If the receiver is already an immutable list, returns it as is.
 *
 * @return an immutable list containing all elements of this collection.
 */
public fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: this.toPersistentList()

/**
 * Converts this array to an immutable list.
 *
 * @return an immutable list containing all elements of this array.
 */
public fun <T> Array<out T>.toImmutableList(): ImmutableList<T> = toPersistentList()

/**
 * Converts this sequence to an immutable list.
 *
 * @return an immutable list containing all elements of this sequence.
 */
public fun <T> Sequence<T>.toImmutableList(): ImmutableList<T> = toPersistentList()

/**
 * Converts this char sequence to an immutable list.
 *
 * @return an immutable list containing all characters.
 */
public fun CharSequence.toImmutableList(): ImmutableList<Char> = toPersistentList()


/**
 * Converts this collection to a persistent list.
 *
 * If the receiver is already a persistent list, returns it as is.
 * If the receiver is a persistent list builder, calls `build` on it and returns the result.
 *
 * @return a persistent list containing all elements of this collection.
 */
public fun <T> Iterable<T>.toPersistentList(): PersistentList<T> =
        this as? PersistentList
        ?: (this as? PersistentList.Builder)?.build()
        ?: persistentListOf<T>() + this

/**
 * Converts this array to a persistent list.
 *
 * @return a persistent list containing all elements of this array.
 */
public fun <T> Array<out T>.toPersistentList(): PersistentList<T> = persistentListOf<T>() + this

/**
 * Converts this sequence to a persistent list.
 *
 * @return a persistent list containing all elements of this sequence.
 */
public fun <T> Sequence<T>.toPersistentList(): PersistentList<T> = persistentListOf<T>() + this

/**
 * Converts this char sequence to a persistent list.
 *
 * @return a persistent list containing all characters.
 */
public fun CharSequence.toPersistentList(): PersistentList<Char> =
    persistentListOf<Char>().mutate { this.toCollection(it) }


/**
 * Converts this collection to an immutable set.
 *
 * If the receiver is already an immutable set, returns it as is.
 *
 * Elements of the returned set are iterated in the same order as in this collection.
 *
 * @return an immutable set of all elements of this collection.
 */
public fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        this as? ImmutableSet<T>
        ?: (this as? PersistentSet.Builder)?.build()
        ?: persistentSetOf<T>() + this

/**
 * Converts this array to an immutable set.
 *
 * Elements of the returned set are iterated in the same order as in this array.
 *
 * @return an immutable set of all elements of this array.
 */
public fun <T> Array<out T>.toImmutableSet(): ImmutableSet<T> = toPersistentSet()

/**
 * Converts this sequence to an immutable set.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 *
 * @return an immutable set of all elements of this sequence.
 */
public fun <T> Sequence<T>.toImmutableSet(): ImmutableSet<T> = toPersistentSet()

/**
 * Converts this char sequence to an immutable set.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 *
 * @return an immutable set of all characters.
 */
public fun CharSequence.toImmutableSet(): PersistentSet<Char> = toPersistentSet()


/**
 * Converts this collection to a persistent set.
 *
 * If the receiver is already a persistent set, returns it as is.
 * If the receiver is a persistent set builder, calls `build` on it and returns the result.
 *
 * Elements of the returned set are iterated in the same order as in this collection.
 *
 * @return a persistent set of all elements of this collection.
 */
public fun <T> Iterable<T>.toPersistentSet(): PersistentSet<T> =
        this as? PersistentOrderedSet<T>
        ?: (this as? PersistentOrderedSetBuilder)?.build()
        ?: PersistentOrderedSet.emptyOf<T>() + this

/**
 * Converts this array to a persistent set.
 *
 * Elements of the returned set are iterated in the same order as in this array.
 *
 * @return a persistent set of all elements of this array.
 */
public fun <T> Array<out T>.toPersistentSet(): PersistentSet<T> = persistentSetOf<T>() + this

/**
 * Converts this sequence to a persistent set.
 *
 * Elements of the returned set are iterated in the same order as in this sequence.
 *
 * @return a persistent set of all elements of this sequence.
 */
public fun <T> Sequence<T>.toPersistentSet(): PersistentSet<T> = persistentSetOf<T>() + this

/**
 * Converts this char sequence to a persistent set.
 *
 * Elements of the returned set are iterated in the same order as in this char sequence.
 *
 * @return a persistent set of all characters.
 */
public fun CharSequence.toPersistentSet(): PersistentSet<Char> =
        persistentSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Converts this collection to a persistent hash set.
 *
 * If the receiver is already a persistent hash set, returns it as is.
 * If the receiver is a persistent hash set builder, calls `build` on it and returns the result.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a persistent set containing all elements from this collection.
 */
public fun <T> Iterable<T>.toPersistentHashSet(): PersistentSet<T>
    = this as? PersistentHashSet
        ?: (this as? PersistentHashSetBuilder<T>)?.build()
        ?: PersistentHashSet.emptyOf<T>() + this

/**
 * Converts this array to a persistent hash set.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a persistent set of all elements of this array.
 */
public fun <T> Array<out T>.toPersistentHashSet(): PersistentSet<T> = persistentHashSetOf<T>() + this

/**
 * Converts this sequence to a persistent hash set.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a persistent set of all elements of this sequence.
 */
public fun <T> Sequence<T>.toPersistentHashSet(): PersistentSet<T> = persistentHashSetOf<T>() + this

/**
 * Converts this char sequence to a persistent hash set.
 *
 * Order of the elements in the returned set is unspecified.
 *
 * @return a persistent set of all characters.
 */
public fun CharSequence.toPersistentHashSet(): PersistentSet<Char> =
        persistentHashSetOf<Char>().mutate { this.toCollection(it) }


/**
 * Converts this map to an immutable map.
 *
 * If the receiver is already an immutable map, returns it as is.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 *
 * @return an immutable map containing all entries from this map.
 */
public fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? PersistentMap.Builder)?.build()
        ?: persistentMapOf<K, V>().puttingAll(this)

/**
 * Converts this map to a persistent map.
 *
 * If the receiver is already a persistent map, returns it as is.
 * If the receiver is a persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 *
 * @return a persistent map containing all entries from this map.
 */
public fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentOrderedMap<K, V>
        ?: (this as? PersistentOrderedMapBuilder<K, V>)?.build()
        ?: PersistentOrderedMap.emptyOf<K, V>().puttingAll(this)

/**
 * Converts this map to a persistent hash map.
 *
 * If the receiver is already a persistent hash map, returns it as is.
 * If the receiver is a persistent hash map builder, calls `build` on it and returns the result.
 *
 * Order of the entries in the returned map is unspecified.
 *
 * @return a persistent map containing all entries from this map.
 */
public fun <K, V> Map<K, V>.toPersistentHashMap(): PersistentMap<K, V>
        = this as? PersistentHashMap
        ?: (this as? PersistentHashMapBuilder<K, V>)?.build()
        ?: PersistentHashMap.emptyOf<K, V>().puttingAll(this)
