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

internal class PersistentOrderedMapBuilder<K, V>(private var map: PersistentOrderedMap<K, V>) : AbstractMutableMap<K, V>(), PersistentMap.Builder<K, V> {
    internal var firstKey = map.firstKey
        private set

    private var lastKey = map.lastKey

    internal val hashMapBuilder = map.hashMap.builder()

    override val size: Int get() = hashMapBuilder.size

    override fun build(): PersistentMap<K, V> {
        val newHashMap = hashMapBuilder.build()
        map = if (newHashMap === map.hashMap) {
            assert(firstKey === map.firstKey)
            assert(lastKey === map.lastKey)
            map
        } else {
            PersistentOrderedMap(firstKey, lastKey, newHashMap)
        }
        return map
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
        if (links != null) {
            if (links.value === value) {
                return value
            }
            hashMapBuilder[key] = links.withValue(value)
            return links.value
        }

        if (isEmpty()) {  //  isEmpty
            firstKey = key
            lastKey = key
            hashMapBuilder[key] = LinkedValue(value)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        val lastKey = lastKey as K
        val lastLinks = hashMapBuilder[lastKey]!!
        assert(!lastLinks.hasNext)

        hashMapBuilder[lastKey] = lastLinks.withNext(key)
        hashMapBuilder[key] = LinkedValue(value, previous = lastKey)
        this.lastKey = key
        return null
    }

    override fun remove(key: K): V? {
        val links = hashMapBuilder.remove(key) ?: return null

        if (links.hasPrevious) {
            val previousLinks = hashMapBuilder[links.previous]!!
//            assert(previousLinks.next == key)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.previous as K] = previousLinks.withNext(links.next)
        } else {
            firstKey = links.next
        }
        if (links.hasNext) {
            val nextLinks = hashMapBuilder[links.next]!!
//            assert(nextLinks.previous == key)
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
        hashMapBuilder.clear()
        firstKey = EndOfChain
        lastKey = EndOfChain
    }

    private fun <K1, V1> containsEntry(entry: Map.Entry<K1, V1>): Boolean {
        entry is Map.Entry<*, *> || return false
        val (k, v) = entry
        val thisValue = get(k)
        return when {
            thisValue === null -> containsKey(k)
            else -> thisValue == v
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedMap<*, *> -> {
                hashMapBuilder.node.equalsWith(other.hashMap.node) { a, b ->
                    a.value == b.value
                }
            }
            is PersistentOrderedMapBuilder<*, *> -> {
                hashMapBuilder.node.equalsWith(other.hashMapBuilder.node) { a, b ->
                    a.value == b.value
                }
            }
            is PersistentHashMap<*, *> -> {
                hashMapBuilder.node.equalsWith(other.node) { a, b ->
                    a.value == b
                }
            }
            is PersistentHashMapBuilder<*, *> -> {
                hashMapBuilder.node.equalsWith(other.node) { a, b ->
                    a.value == b
                }
            }
            // should be super.equals(other), but https://youtrack.jetbrains.com/issue/KT-45673
            else -> other.entries.all { entry -> containsEntry(entry) }
        }
    }

}