/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapImpl

/**
 * Returns a new persistent map with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable map passed to the [mutator] closure has the same contents as this persistent map.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <K, V> PersistentOrderedMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentOrderedMap<K, V> =
    (this as PersistentOrderedMap<K, V>).builder().apply(mutator).build()

/**
 * Returns a new persistent map with an entry from the specified key-value [pair] added,
 * or this instance if no modifications were made in the result of this operation.
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 */
@Suppress("UNCHECKED_CAST")
public inline operator fun <K, V> PersistentOrderedMap<out K, V>.plus(pair: Pair<K, V>): PersistentOrderedMap<K, V> =
    (this as PersistentOrderedMap<K, V>).putting(pair.first, pair.second)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentOrderedMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentOrderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentOrderedMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentOrderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public inline operator fun <K, V> PersistentOrderedMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentOrderedMap<K, V> =
    puttingAll(pairs)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `putting(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 */
public inline operator fun <K, V> PersistentOrderedMap<out K, V>.plus(map: Map<out K, V>): PersistentOrderedMap<K, V> =
    puttingAll(map)

/**
 * Returns a new persistent map with keys and values from the specified [map] associated,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The effect of this call is equivalent to that of calling `putting(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> PersistentOrderedMap<out K, V>.puttingAll(map: Map<out K, V>): PersistentOrderedMap<K, V> =
    (this as PersistentOrderedMap<K, V>).puttingAll(map)

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentOrderedMap<out K, V>.puttingAll(pairs: Iterable<Pair<K, V>>): PersistentOrderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentOrderedMap<out K, V>.puttingAll(pairs: Array<out Pair<K, V>>): PersistentOrderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with entries from the specified key-value pairs added,
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentOrderedMap<out K, V>.puttingAll(pairs: Sequence<Pair<K, V>>): PersistentOrderedMap<K, V> =
    mutate { it.putAll(pairs) }

/**
 * Returns a new persistent map with the specified [key] and its corresponding value removed,
 * or this instance if it contains no mapping for the key.
 */
@Suppress("UNCHECKED_CAST")
public operator fun <K, V> PersistentOrderedMap<out K, V>.minus(key: K): PersistentOrderedMap<K, V> =
    (this as PersistentOrderedMap<K, V>).removing(key)

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] collection,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentOrderedMap<out K, V>.minus(keys: Iterable<K>): PersistentOrderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] array,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentOrderedMap<out K, V>.minus(keys: Array<out K>): PersistentOrderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map containing all entries of this map
 * except those whose keys are contained in the specified [keys] sequence,
 * or this instance if there are no entries to remove.
 */
public operator fun <K, V> PersistentOrderedMap<out K, V>.minus(keys: Sequence<K>): PersistentOrderedMap<K, V> =
    mutate { it.minusAssign(keys) }

/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
public fun <K, V> persistentOrderedMapOf(vararg pairs: Pair<K, V>): PersistentOrderedMap<K, V> =
    PersistentOrderedMapImpl.emptyOf<K, V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
public fun <K, V> persistentOrderedMapOf(): PersistentOrderedMap<K, V> = PersistentOrderedMapImpl.emptyOf()

/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already an ordered persistent map, returns it as is.
 * If the receiver is an ordered persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 */
public fun <K, V> Map<K, V>.toPersistentOrderedMap(): PersistentOrderedMap<K, V> =
    this as? PersistentOrderedMap<K, V>
        ?: (this as? PersistentOrderedMap.Builder<K, V>)?.build()
        ?: PersistentOrderedMapImpl.emptyOf<K, V>().puttingAll(this)
