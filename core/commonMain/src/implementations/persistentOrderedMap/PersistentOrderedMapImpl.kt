/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class LinkedValue<V>(val value: V, val previous: Any?, val next: Any?) {
    /** Constructs LinkedValue for a new single entry */
    constructor(value: V) : this(value, EndOfChain, EndOfChain)

    /** Constructs LinkedValue for a new last entry */
    constructor(value: V, previous: Any?) : this(value, previous, EndOfChain)

    fun withValue(newValue: V) = LinkedValue(newValue, previous, next)
    fun withPrevious(newPrevious: Any?) = LinkedValue(value, newPrevious, next)
    fun withNext(newNext: Any?) = LinkedValue(value, previous, newNext)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain
}

internal class PersistentOrderedMapImpl<K, V>(
    internal val firstKey: Any?,
    internal val lastKey: Any?,
    internal val hashMap: PersistentHashMap<K, LinkedValue<V>>
) : AbstractMap<K, V>(), PersistentOrderedMap<K, V> {

    override val size: Int get() = hashMap.size

    override val keys: ImmutableSet<K>
        get() {
            return PersistentOrderedMapKeys(this)
        }

    override val values: ImmutableCollection<V>
        get() {
            return PersistentOrderedMapValues(this)
        }

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() {
            return createEntries()
        }

    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> {
        return PersistentOrderedMapEntries(this)
    }

    // TODO: compiler bug: this bridge should be generated automatically, KT-20070
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> {
        return createEntries()
    }

    override fun containsKey(key: K): Boolean = hashMap.containsKey(key)

    override fun get(key: K): V? = hashMap[key]?.value

    override fun putting(key: K, value: @UnsafeVariance V): PersistentOrderedMapImpl<K, V> {
        if (isEmpty()) {
            val newMap = hashMap.putting(key, LinkedValue(value))
            return PersistentOrderedMapImpl(key, key, newMap)
        }

        val links = hashMap[key]
        if (links != null) {
            if (links.value === value) {
                return this
            }
            val newMap = hashMap.putting(key, links.withValue(value))
            return PersistentOrderedMapImpl(firstKey, lastKey, newMap)
        }

        @Suppress("UNCHECKED_CAST")
        val lastKey = lastKey as K
        val lastLinks = hashMap[lastKey]!!
        assert { !lastLinks.hasNext }
        val newMap = hashMap
            .putting(lastKey, lastLinks.withNext(key))
            .putting(key, LinkedValue(value, previous = lastKey))
        return PersistentOrderedMapImpl(firstKey, key, newMap)
    }

    override fun removing(key: K): PersistentOrderedMapImpl<K, V> {
        val links = hashMap[key] ?: return this

        var newMap = hashMap.removing(key)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
            assert { previousLinks.next == key }
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.putting(links.previous as K, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
            assert { nextLinks.previous == key }
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.putting(links.next as K, nextLinks.withPrevious(links.previous))
        }

        val newFirstKey = if (!links.hasPrevious) links.next else firstKey
        val newLastKey = if (!links.hasNext) links.previous else lastKey
        return PersistentOrderedMapImpl(newFirstKey, newLastKey, newMap)
    }

    override fun removing(key: K, value: @UnsafeVariance V): PersistentOrderedMapImpl<K, V> {
        val links = hashMap[key] ?: return this
        return if (links.value == value) this.removing(key) else this
    }

    override fun puttingAll(m: Map<out K, @UnsafeVariance V>): PersistentOrderedMapImpl<K, V> {
        if (m.isEmpty()) return this
        return builder().apply { putAll(m) }.build()
    }

    override fun cleared(): PersistentOrderedMapImpl<K, V> {
        return emptyOf()
    }

    override fun builder(): PersistentOrderedMapBuilder<K, V> {
        return PersistentOrderedMapBuilder(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedMapImpl<*, *> -> hashMap.node.equalsWith(other.hashMap.node) { a, b -> a.value == b.value }
            is PersistentOrderedMapBuilder<*, *> ->
                hashMap.node.equalsWith(other.hashMapBuilder.node) { a, b -> a.value == b.value }
            is PersistentHashMap<*, *> -> hashMap.node.equalsWith(other.node) { a, b -> a.value == b }
            is PersistentHashMapBuilder<*, *> -> hashMap.node.equalsWith(other.node) { a, b -> a.value == b }
            else -> super.equals(other)
        }
    }

    /**
     * We provide [equals], so as a matter of style, we should also provide [hashCode].
     * However, the implementation from [AbstractMap] is enough.
     */
    override fun hashCode(): Int = super<AbstractMap>.hashCode()

    internal companion object {
        private val EMPTY = PersistentOrderedMapImpl<Nothing, Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf())

        @Suppress("UNCHECKED_CAST")
        internal fun <K, V> emptyOf(): PersistentOrderedMapImpl<K, V> = EMPTY as PersistentOrderedMapImpl<K, V>
    }
}
