/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent map that iterates its entries in key insertion order.
 *
 * Adding a new key appends its entry to the iteration order. Replacing the value of an existing key
 * does not change its position. Removing a key preserves the relative order of the remaining entries;
 * removing and then adding it again places its entry at the end. Bulk additions process entries in
 * the iteration order of the source map.
 *
 * The [keys], [values], and [entries] views follow the same iteration order.
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
     * A reusable builder of an ordered persistent map.
     *
     * The builder and its keys, values, and entries views maintain insertion order according to the
     * [PersistentOrderedMap] contract. Modifications do not affect previously built maps.
     */
    public interface Builder<K, V> : PersistentMap.Builder<K, V> {
        override fun build(): PersistentOrderedMap<K, V>
    }
}
