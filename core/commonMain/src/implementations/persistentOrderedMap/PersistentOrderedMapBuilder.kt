/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class PersistentOrderedMapBuilder<K, V>(map: PersistentOrderedMap<K, V>) :
    AbstractMutableMap<K, V>(), PersistentMap.Builder<K, V> {
    private var builtMap: PersistentOrderedMap<K, V>? = map

    internal var firstKey = map.firstKey
        private set

    private var lastKey = map.lastKey

    internal val hashMapBuilder = map.hashMap.builder()

    override val size: Int get() = hashMapBuilder.size

    override fun build(): PersistentMap<K, V> {
        return builtMap?.also { map ->
            assert { hashMapBuilder.builtMap != null }
            assert { firstKey === map.firstKey }
            assert { lastKey === map.lastKey }
        } ?: run {
            assert { hashMapBuilder.builtMap == null }
            val newHashMap = hashMapBuilder.build()
            val newOrdered = PersistentOrderedMap(firstKey, lastKey, newHashMap)
            builtMap = newOrdered
            newOrdered
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return PersistentOrderedMapBuilderEntries(this)
        }

    override val keys: MutableSet<K>
        get() {
            return PersistentOrderedMapBuilderKeys(this)
        }

    override val values: MutableCollection<V>
        get() {
            return PersistentOrderedMapBuilderValues(this)
        }

    override fun containsKey(key: K): Boolean = hashMapBuilder.containsKey(key)

    override fun get(key: K): V? = hashMapBuilder[key]?.value

    override fun put(key: K, value: @UnsafeVariance V): V? {
        val links = hashMapBuilder[key]
        return if (links != null) {
            if (links.value === value) {
                value
            } else {
                hashMapBuilder[key] = links.withValue(value)
                builtMap = null
                links.value
            }
        } else {
            if (isEmpty()) {
                hashMapBuilder[key] = LinkedValue(value)
                builtMap = null
                firstKey = key
                lastKey = key
            } else {
                @Suppress("UNCHECKED_CAST")
                val lastKey = lastKey as K
                val lastLinks = hashMapBuilder[lastKey]!!
                assert { !lastLinks.hasNext }
                hashMapBuilder[key] = LinkedValue(value, previous = lastKey)
                builtMap = null
                hashMapBuilder[lastKey] = lastLinks.withNext(key)
                this.lastKey = key
            }
            null
        }
    }

    internal fun setLinkedValue(key: K, links: LinkedValue<V>) {
        hashMapBuilder[key] = links
        builtMap = null
    }

    override fun remove(key: K): V? {
        val links = hashMapBuilder[key] ?: return null
        val previousLinks = if (links.hasPrevious) hashMapBuilder[links.previous]!! else null
        val nextLinks = if (links.hasNext) hashMapBuilder[links.next]!! else null
        assert { previousLinks == null || previousLinks.next == key }
        assert { nextLinks == null || nextLinks.previous == key }
        hashMapBuilder.remove(key) ?: return null
        builtMap = null
        if (previousLinks != null) {
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.previous as K] = previousLinks.withNext(links.next)
        } else {
            firstKey = links.next
        }
        if (nextLinks != null) {
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.next as K] = nextLinks.withPrevious(links.previous)
        } else {
            lastKey = links.previous
        }

        return links.value
    }

    fun remove(key: K, value: V): Boolean {
        val links = hashMapBuilder[key] ?: return false

        return if (links.value != value) {
            false
        } else {
            remove(key)
            true
        }
    }

    override fun clear() {
        if (hashMapBuilder.isNotEmpty()) {
            builtMap = null
        }
        hashMapBuilder.clear()
        firstKey = EndOfChain
        lastKey = EndOfChain
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedMap<*, *> ->
                hashMapBuilder.node.equalsWith(other.hashMap.node) { a, b -> a.value == b.value }
            is PersistentOrderedMapBuilder<*, *> ->
                hashMapBuilder.node.equalsWith(other.hashMapBuilder.node) { a, b -> a.value == b.value }
            is PersistentHashMap<*, *> -> hashMapBuilder.node.equalsWith(other.node) { a, b -> a.value == b }
            is PersistentHashMapBuilder<*, *> -> hashMapBuilder.node.equalsWith(other.node) { a, b -> a.value == b }
            else -> super.equals(other)
        }
    }

    /**
     * We provide [equals], so as a matter of style, we should also provide [hashCode].
     * However, the implementation from [AbstractMutableMap] is enough.
     */
    override fun hashCode(): Int = super.hashCode()
}
