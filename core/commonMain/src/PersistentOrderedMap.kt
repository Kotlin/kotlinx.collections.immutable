/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent map that iterates its entries in the key insertion order.
 *
 * The map maintains a predictable iteration order for its keys, values, and entries: entries are
 * iterated in the order their keys were inserted into the map, from oldest to newest. The insertion
 * order is not affected if a key is re-inserted, i.e. when [putting] is called with a key the map
 * already contains.
 *
 * Modification operations return ordered maps. Equality and hash codes follow the [Map] contract
 * and do not depend on iteration order.
 *
 * @param K the type of map keys. The map is invariant on its key type.
 * @param V the type of map values. The map is covariant on its value type.
 */
public interface PersistentOrderedMap<K, out V> : PersistentMap<K, V> {
    override fun putting(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V>

    override fun puttingAll(m: Map<out K, @UnsafeVariance V>): PersistentOrderedMap<K, V>

    override fun removing(key: K): PersistentOrderedMap<K, V>

    override fun removing(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V>

    override fun cleared(): PersistentOrderedMap<K, V>

    override fun builder(): Builder<K, @UnsafeVariance V>

    /**
     * A builder of a [PersistentOrderedMap].
     *
     * [PersistentOrderedMap.Builder] extends the [PersistentMap.Builder] contract with the iteration order
     * guarantee of [PersistentOrderedMap]: the builder's keys, values, and entries are iterated in the key
     * insertion order, and [build] returns a map with the same iteration order.
     */
    public interface Builder<K, V> : PersistentMap.Builder<K, V> {
        override fun build(): PersistentOrderedMap<K, V>
    }
}
