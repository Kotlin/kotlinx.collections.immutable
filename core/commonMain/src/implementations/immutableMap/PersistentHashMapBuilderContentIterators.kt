/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.internal.assert


internal class TrieNodeMutableEntriesIterator<K, V>(
    private val builder: PersistentHashMapBuilder<K, V>
) : TrieNodeBaseIterator<K, V, MutableMap.MutableEntry<K, V>>() {

    override fun next(): MutableMap.MutableEntry<K, V> {
        assert { hasNextKey() }
        index += 2
        @Suppress("UNCHECKED_CAST")
        return MutableMapEntry(builder, buffer[index - 2] as K, buffer, index - 2)
    }
}

private class MutableMapEntry<K, V>(
    private val builder: PersistentHashMapBuilder<K, V>,
    override val key: K,
    override var buffer: Array<Any?>,
    override var index: Int
) : AbstractMapEntry<K, V>(), TrieNodeSlot, MutableMap.MutableEntry<K, V> {
    private var slotModCount = builder.modCount
    private var lastValue = slotValue()

    override val value: V
        get() {
            ensureSlotIsLive()
            return lastValue
        }

    override fun setValue(newValue: V): V {
        ensureSlotIsLive()
        val result = lastValue
        if (index != -1) builder[key] = newValue
        lastValue = newValue
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun slotValue() = buffer[index + 1] as V

    private fun ensureSlotIsLive() {
        if (builder.modCount != slotModCount) {
            slotModCount = builder.modCount
            if (!builder.node.locate(key.hashCode(), key, 0, this)) {
                buffer = TrieNode.EMPTY.buffer
                index = -1
            }
        }
        if (index != -1) lastValue = slotValue()
    }
}


internal open class PersistentHashMapBuilderBaseIterator<K, V, T>(
    private val builder: PersistentHashMapBuilder<K, V>,
    path: Array<TrieNodeBaseIterator<K, V, T>>
) : MutableIterator<T>, PersistentHashMapBaseIterator<K, V, T>(builder.node, path) {

    private var lastIteratedKey: K? = null
    private var nextWasInvoked = false
    private var expectedSizeModCount = builder.sizeModCount
    private var pathModCount = builder.modCount

    override fun next(): T {
        checkForComodification()
        ensurePathIsLive()
        lastIteratedKey = currentKey()
        nextWasInvoked = true
        return super.next()
    }

    override fun remove() {
        checkNextWasInvoked()
        checkForComodification()
        builder.remove(lastIteratedKey)
        lastIteratedKey = null
        nextWasInvoked = false
        expectedSizeModCount = builder.sizeModCount
    }

    private fun ensurePathIsLive() {
        if (builder.modCount == pathModCount) return

        if (hasNext()) {
            val currentKey = currentKey()
            resetPath(currentKey.hashCode(), builder.node, currentKey, 0)
        }
        pathModCount = builder.modCount
    }

    private fun resetPath(keyHash: Int, node: TrieNode<*, *>, key: K, pathIndex: Int) {
        val shift = pathIndex * LOG_MAX_BRANCHING_FACTOR

        if (shift > MAX_SHIFT) { // collision
            val keyIndex = node.collisionKeyIndex(key)
            if (keyIndex == -1) throw hashCodeChanged(key)
            path[pathIndex].reset(node.buffer, node.buffer.size, keyIndex)
            pathLastIndex = pathIndex
            return
        }

        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (node.hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = node.entryKeyIndex(keyPositionMask)
            val storedKey = node.keyAtIndex(keyIndex)
            if (storedKey !== key && key != storedKey) throw hashCodeChanged(key)

            if (pathIndex < pathLastIndex) {
                // The key was promoted out of a removed subnode into the entries this level has passed:
                // return it once as a one-entry level, then go on with the subnodes after the removed one.
                val slot = node.nodeIndex(keyPositionMask)
                path[pathIndex].reset(node.buffer, slot, slot)
                path[pathIndex + 1].reset(node.buffer, keyIndex + ENTRY_SIZE, keyIndex)
                pathLastIndex = pathIndex + 1
                pathEndsWithEntry = true
                return
            }

            path[pathIndex].reset(node.buffer, ENTRY_SIZE * node.entryCount(), keyIndex)
            pathLastIndex = pathIndex
            return
        }

        if (!node.hasNodeAt(keyPositionMask)) throw hashCodeChanged(key)

        val nodeIndex = node.nodeIndex(keyPositionMask)
        val targetNode = node.nodeAtIndex(nodeIndex)
        resetPath(keyHash, targetNode, key, pathIndex + 1)
        path[pathIndex].reset(node.buffer, ENTRY_SIZE * node.entryCount(), nodeIndex)
    }

    private fun hashCodeChanged(key: K) =
        ConcurrentModificationException("Hash code of a key ($key) has changed after it was added to the persistent map.")

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.sizeModCount != expectedSizeModCount)
            throw ConcurrentModificationException()
    }
}

internal class PersistentHashMapBuilderEntriesIterator<K, V>(builder: PersistentHashMapBuilder<K, V>) :
    PersistentHashMapBuilderBaseIterator<K, V, MutableMap.MutableEntry<K, V>>(
        builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeMutableEntriesIterator(builder) }
    )

internal class PersistentHashMapBuilderKeysIterator<K, V>(builder: PersistentHashMapBuilder<K, V>) :
    PersistentHashMapBuilderBaseIterator<K, V, K>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeKeysIterator() })

internal class PersistentHashMapBuilderValuesIterator<K, V>(builder: PersistentHashMapBuilder<K, V>) :
    PersistentHashMapBuilderBaseIterator<K, V, V>(
        builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeValuesIterator() }
    )
