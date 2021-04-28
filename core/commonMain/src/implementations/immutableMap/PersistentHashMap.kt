/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import kotlinx.collections.immutable.mutate

internal class PersistentHashMap<K, V>(internal val node: TrieNode<K, V>,
                                       override val size: Int): AbstractMap<K, V>(), PersistentMap<K, V> {

    override val keys: ImmutableSet<K>
        get() {
            return PersistentHashMapKeys(this)
        }

    override val values: ImmutableCollection<V>
        get() {
            return PersistentHashMapValues(this)
        }

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() {
            return createEntries()
        }

    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> {
        return PersistentHashMapEntries(this)
    }

    // TODO: compiler bug: this bridge should be generated automatically
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> {
        return createEntries()
    }

    override fun containsKey(key: K): Boolean {
        return node.containsKey(key.hashCode(), key, 0)
    }

    override fun get(key: K): V? {
        return node.get(key.hashCode(), key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): PersistentHashMap<K, V> {
        val newNodeResult = node.put(key.hashCode(), key, value, 0) ?: return this
        return PersistentHashMap(newNodeResult.node, size + newNodeResult.sizeDelta)
    }

    override fun remove(key: K): PersistentHashMap<K, V> {
        val newNode = node.remove(key.hashCode(), key, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return emptyOf() }
        return PersistentHashMap(newNode, size - 1)
    }

    override fun remove(key: K, value: @UnsafeVariance V): PersistentHashMap<K, V> {
        val newNode = node.remove(key.hashCode(), key, value, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return emptyOf() }
        return PersistentHashMap(newNode, size - 1)
    }

    override fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V> {
        return this.mutate { it.putAll(m) }
    }

    override fun clear(): PersistentMap<K, V> {
        return PersistentHashMap.emptyOf()
    }

    override fun builder(): PersistentHashMapBuilder<K, V> {
        return PersistentHashMapBuilder(this)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PersistentOrderedMap<*, *> -> {
                node.equalsWith(other.hashMap.node) { a, b ->
                    a == b.value
                }
            }
            is PersistentOrderedMapBuilder<*, *> -> {
                node.equalsWith(other.hashMapBuilder.node) { a, b ->
                    a == b.value
                }
            }
            is PersistentHashMap<*, *> -> {
                node.equalsWith(other.node) { a, b -> a == b }
            }
            is PersistentHashMapBuilder<*, *> -> {
                node.equalsWith(other.node) { a, b -> a == b }
            }
            else -> super.equals(other)
        }
    }

    internal companion object {
        private val EMPTY = PersistentHashMap(TrieNode.EMPTY, 0)
        @Suppress("UNCHECKED_CAST")
        internal fun <K, V> emptyOf(): PersistentHashMap<K, V> = EMPTY as PersistentHashMap<K, V>
    }
}