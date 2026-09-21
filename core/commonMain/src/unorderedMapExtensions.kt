/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap

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
