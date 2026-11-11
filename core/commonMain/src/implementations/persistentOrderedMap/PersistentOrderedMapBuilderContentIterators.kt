/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.implementations.immutableMap.AbstractMapEntry
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import kotlinx.collections.immutable.implementations.immutableMap.TrieNodeSlot
import kotlinx.collections.immutable.internal.EndOfChain

internal open class PersistentOrderedMapBuilderLinksIterator<K, V>(
    private var nextKey: Any?,
    internal val builder: PersistentOrderedMapBuilder<K, V>
) : MutableIterator<LinkedValue<V>>, TrieNodeSlot {

    internal var lastIteratedKey: Any? = EndOfChain
    private var nextWasInvoked = false
    private var expectedSizeModCount = builder.hashMapBuilder.sizeModCount
    override var buffer: Array<Any?> = TrieNode.EMPTY.buffer
    override var index = -1

    override fun hasNext(): Boolean {
        return nextKey !== EndOfChain
    }

    @IgnorableReturnValue
    override fun next(): LinkedValue<V> {
        checkForComodification()
        checkHasNext()
        lastIteratedKey = nextKey
        nextWasInvoked = true
        @Suppress("UNCHECKED_CAST")
        val key = nextKey as K
        if (!builder.hashMapBuilder.node.locate(key.hashCode(), key, 0, this)) {
            throw ConcurrentModificationException("Hash code of a key ($nextKey) has changed after it was added to the persistent map.")
        }
        @Suppress("UNCHECKED_CAST")
        val result = buffer[index + 1] as LinkedValue<V>
        nextKey = result.next
        return result
    }

    override fun remove() {
        checkNextWasInvoked()
        checkForComodification()
        builder.remove(lastIteratedKey)
        lastIteratedKey = null
        nextWasInvoked = false
        expectedSizeModCount = builder.hashMapBuilder.sizeModCount
    }

    private fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.hashMapBuilder.sizeModCount != expectedSizeModCount)
            throw ConcurrentModificationException()
    }
}

internal class PersistentOrderedMapBuilderEntriesIterator<K, V>(map: PersistentOrderedMapBuilder<K, V>) :
    MutableIterator<MutableMap.MutableEntry<K, V>> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): MutableMap.MutableEntry<K, V> {
        internal.next()
        @Suppress("UNCHECKED_CAST")
        return MutableMapEntry(internal.builder, internal.lastIteratedKey as K, internal.buffer, internal.index)
    }

    override fun remove() {
        internal.remove()
    }
}

private class MutableMapEntry<K, V>(
    private val builder: PersistentOrderedMapBuilder<K, V>,
    override val key: K,
    override var buffer: Array<Any?>,
    override var index: Int
) : AbstractMapEntry<K, V>(), TrieNodeSlot, MutableMap.MutableEntry<K, V> {
    private var expectedModCount = builder.hashMapBuilder.modCount
    private var lastValue = links().value

    override val value: V
        get() {
            ensureSlotIsLive()
            return lastValue
        }

    override fun setValue(newValue: V): V {
        ensureSlotIsLive()
        val result = lastValue
        lastValue = newValue
        if (index != -1) {
            val links = links()
            if (links.value !== newValue) builder.setLinkedValue(key, links.withValue(newValue))
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun links() = buffer[index + 1] as LinkedValue<V>

    private fun ensureSlotIsLive() {
        val hashMapBuilder = builder.hashMapBuilder
        if (hashMapBuilder.modCount != expectedModCount) {
            expectedModCount = hashMapBuilder.modCount
            if (!hashMapBuilder.node.locate(key.hashCode(), key, 0, this)) {
                buffer = TrieNode.EMPTY.buffer
                index = -1
            }
        }
        if (index != -1) lastValue = links().value
    }
}

internal class PersistentOrderedMapBuilderKeysIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>) :
    MutableIterator<K> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): K {
        internal.next()
        @Suppress("UNCHECKED_CAST")
        return internal.lastIteratedKey as K
    }

    override fun remove() {
        internal.remove()
    }
}

internal class PersistentOrderedMapBuilderValuesIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>) :
    MutableIterator<V> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): V {
        return internal.next().value
    }

    override fun remove() {
        internal.remove()
    }
}
