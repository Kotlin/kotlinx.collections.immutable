/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.implementations.immutableMap.MapEntry
import kotlinx.collections.immutable.internal.EndOfChain

internal open class PersistentOrderedMapBuilderLinksIterator<K, V>(
    private var nextKey: Any?,
    internal val builder: PersistentOrderedMapBuilder<K, V>
) : MutableIterator<LinkedValue<V>> {

    internal var lastIteratedKey: Any? = EndOfChain
    private var nextWasInvoked = false
    private var expectedModCount = builder.hashMapBuilder.sizeModCount
    private var remaining = builder.size

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
        val result = builder.hashMapBuilder.getOrElse(nextKey as K) {
            throw ConcurrentModificationException("Hash code of a key ($nextKey) has changed after it was added to the persistent map.")
        }
        nextKey = result.next
        remaining--
        checkChainMatchesSize()
        return result
    }

    override fun remove() {
        checkNextWasInvoked()
        // The value may be null, so only the size tells whether the key was found.
        val size = builder.size
        builder.remove(lastIteratedKey)
        if (builder.size == size)
            throw ConcurrentModificationException("Hash code of a key ($lastIteratedKey) has changed after it was added to the persistent map.")
        lastIteratedKey = null
        nextWasInvoked = false
        expectedModCount = builder.hashMapBuilder.sizeModCount
    }

    private fun checkHasNext() {
        if (!hasNext())
            throw NoSuchElementException()
    }

    private fun checkChainMatchesSize() {
        // A key that became equal to another one makes the chain loop or end early.
        if ((nextKey === EndOfChain) != (remaining == 0)) {
            // A caller going on after the exception must not loop.
            nextKey = EndOfChain
            throw ConcurrentModificationException("A key has become equal to another key after it was added to the persistent map.")
        }
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.hashMapBuilder.sizeModCount != expectedModCount)
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
        val links = internal.next()
        @Suppress("UNCHECKED_CAST")
        return MutableMapEntry(internal.builder, internal.lastIteratedKey as K, links)
    }

    override fun remove() {
        internal.remove()
    }
}

private class MutableMapEntry<K, V>(
    private val builder: PersistentOrderedMapBuilder<K, V>,
    key: K,
    private var links: LinkedValue<V>
) : MapEntry<K, V>(key, links.value), MutableMap.MutableEntry<K, V> {
    private val expectedModCount = builder.hashMapBuilder.sizeModCount

    override val value: V
        get() = links.value

    override fun setValue(newValue: V): V {
        val result = links.value
        links = links.withValue(newValue)
        if (builder.hashMapBuilder.sizeModCount == expectedModCount) {
            builder.setLinkedValue(key, links)
        } else if (builder.containsKey(key)) {
            builder[key] = newValue
        }
        return result
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
