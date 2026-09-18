/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent map whose entry iteration order is unspecified.
 *
 * This interface expresses that insertion order is not required. Modification operations return
 * unordered maps and may change the relative iteration order of existing entries. Iteration order
 * remains the same for any given immutable map instance.
 *
 * Equality and hash codes follow the [Map] contract and do not depend on iteration order.
 *
 * @param K the type of map keys. The map is invariant on its key type.
 * @param V the type of map values. The map is covariant on its value type.
 */
public interface PersistentUnorderedMap<K, out V> : PersistentMap<K, V> {
    override fun putting(key: K, value: @UnsafeVariance V): PersistentUnorderedMap<K, V>

    override fun puttingAll(m: Map<out K, @UnsafeVariance V>): PersistentUnorderedMap<K, V>

    override fun removing(key: K): PersistentUnorderedMap<K, V>

    override fun removing(key: K, value: @UnsafeVariance V): PersistentUnorderedMap<K, V>

    override fun cleared(): PersistentUnorderedMap<K, V>

    override fun builder(): Builder<K, @UnsafeVariance V>

    /**
     * A reusable builder of an unordered persistent map.
     *
     * Iteration order is unspecified. Modifications do not affect previously built maps.
     */
    public interface Builder<K, V> : PersistentMap.Builder<K, V> {
        override fun build(): PersistentUnorderedMap<K, V>
    }
}
