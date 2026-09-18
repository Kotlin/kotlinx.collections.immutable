/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet

/**
 * Returns a new persistent set with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable set passed to the [mutator] closure has the same contents as this persistent set.
 */
public inline fun <T> PersistentUnorderedSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentUnorderedSet<T> =
    builder().apply(mutator).build()

/**
 * Returns a new persistent map with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable map passed to the [mutator] closure has the same contents as this persistent map.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <K, V> PersistentUnorderedMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentUnorderedMap<K, V> =
    (this as PersistentUnorderedMap<K, V>).builder().apply(mutator).build()

/**
 * Returns a new persistent set with the specified [element] added,
 * or this instance if it already contains the element.
 */
public inline operator fun <E> PersistentUnorderedSet<E>.plus(element: E): PersistentUnorderedSet<E> = adding(element)

/**
 * Returns a new persistent set with the specified [element] removed,
 * or this instance if there is no such element in this set.
 */
public inline operator fun <E> PersistentUnorderedSet<E>.minus(element: E): PersistentUnorderedSet<E> = removing(element)

/**
 * Returns a new persistent set with elements of the specified [elements] collection added,
 * or this instance if it already contains every element of the specified collection.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] array added,
 * or this instance if it already contains every element of the specified array.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Array<out E>): PersistentUnorderedSet<E> = mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] sequence added,
 * or this instance if it already contains every element of the specified sequence.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Sequence<E>): PersistentUnorderedSet<E> = mutate { it.addAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] collection,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] array,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Array<out E>): PersistentUnorderedSet<E> =
    mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] sequence,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Sequence<E>): PersistentUnorderedSet<E> =
    mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set with elements in this set that are also
 * contained in the specified [elements] collection,
 * or this instance if no modifications were made in the result of this operation.
 */
public infix fun <E> PersistentUnorderedSet<E>.intersect(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) retainingAll(elements) else mutate { it.retainAll(elements) }

/**
 * Returns a new persistent map with an entry from the specified key-value [pair] added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 */
@Suppress("UNCHECKED_CAST")
public inline operator fun <K, V> PersistentUnorderedMap<out K, V>.plus(pair: Pair<K, V>): PersistentUnorderedMap<K, V> =
    (this as PersistentUnorderedMap<K, V>).putting(pair.first, pair.second)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentUnorderedMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentUnorderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentUnorderedMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentUnorderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentUnorderedMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentUnorderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `putting(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 */
public inline operator fun <K, V> PersistentUnorderedMap<out K, V>.plus(map: Map<out K, V>): PersistentUnorderedMap<K, V> =
    puttingAll(map)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `putting(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> PersistentUnorderedMap<out K, V>.puttingAll(map: Map<out K, V>): PersistentUnorderedMap<K, V> =
    (this as PersistentUnorderedMap<K, V>).puttingAll(map)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentUnorderedMap<out K, V>.puttingAll(pairs: Iterable<Pair<K, V>>): PersistentUnorderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentUnorderedMap<out K, V>.puttingAll(pairs: Array<out Pair<K, V>>): PersistentUnorderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentUnorderedMap<out K, V>.puttingAll(pairs: Sequence<Pair<K, V>>): PersistentUnorderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with the specified [key] and its corresponding value removed,
 * or this instance if it contains no mapping for the key.
 */
@Suppress("UNCHECKED_CAST")
public operator fun <K, V> PersistentUnorderedMap<out K, V>.minus(key: K): PersistentUnorderedMap<K, V> =
    (this as PersistentUnorderedMap<K, V>).removing(key)

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] collection,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentUnorderedMap<out K, V>.minus(keys: Iterable<K>): PersistentUnorderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] array,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentUnorderedMap<out K, V>.minus(keys: Array<out K>): PersistentUnorderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] sequence,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentUnorderedMap<out K, V>.minus(keys: Sequence<K>): PersistentUnorderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <E> persistentUnorderedSetOf(vararg elements: E): PersistentUnorderedSet<E> =
    PersistentHashSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Returns an empty persistent set.
 */
public fun <E> persistentUnorderedSetOf(): PersistentUnorderedSet<E> = PersistentHashSet.emptyOf()

/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 */
public fun <K, V> persistentUnorderedMapOf(vararg pairs: Pair<K, V>): PersistentUnorderedMap<K, V> =
    PersistentHashMap.emptyOf<K, V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
public fun <K, V> persistentUnorderedMapOf(): PersistentUnorderedMap<K, V> = PersistentHashMap.emptyOf()

/**
 * Returns a persistent set containing all elements from this iterable.
 *
 * If the receiver is already a persistent unordered set, returns it as is.
 * If the receiver is a persistent unordered set builder, calls `build` on it and returns the result.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Iterable<T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> =
    this as? PersistentUnorderedSet<T>
        ?: (this as? PersistentUnorderedSet.Builder<T>)?.build()
        ?: (PersistentHashSet.emptyOf<T>() + this)

/**
 * Returns a persistent set of all elements of this array.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Array<out T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> = persistentUnorderedSetOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Sequence<T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> = persistentUnorderedSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun CharSequence.toPersistentUnorderedSet(): PersistentUnorderedSet<Char> =
    persistentUnorderedSetOf<Char>().mutate { this.toCollection(it) }

/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already a persistent unordered map, returns it as is.
 * If the receiver is a persistent unordered map builder, calls `build` on it and returns the result.
 *
 * Order of the entries in the returned map is unspecified.
 */
public fun <K, V> Map<K, V>.toPersistentUnorderedMap(): PersistentUnorderedMap<K, V> =
    this as? PersistentUnorderedMap<K, V>
        ?: (this as? PersistentUnorderedMap.Builder<K, V>)?.build()
        ?: PersistentHashMap.emptyOf<K, V>().puttingAll(this)
