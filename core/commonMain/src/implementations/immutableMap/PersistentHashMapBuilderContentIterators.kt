/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.internal.assert


internal class TrieNodeMutableEntriesIterator<K, V>(
        private val parentIterator: PersistentHashMapBuilderEntriesIterator<K, V>
) : TrieNodeBaseIterator<K, V, MutableMap.MutableEntry<K, V>>() {

    override fun next(): MutableMap.MutableEntry<K, V> {
        assert(hasNextKey())
        index += 2
        @Suppress("UNCHECKED_CAST")
        return MutableMapEntry(parentIterator, buffer[index - 2] as K, buffer[index - 1] as V)
    }
}

private class MutableMapEntry<K, V>(
        private val parentIterator: PersistentHashMapBuilderEntriesIterator<K, V>,
        key: K,
        override var value: V
) : MapEntry<K, V>(key, value), MutableMap.MutableEntry<K, V> {

    override fun setValue(newValue: V): V {
        val result = value
        value = newValue
        parentIterator.setValue(key, newValue)
        return result
    }
}


internal open class PersistentHashMapBuilderBaseIterator<K, V, T>(
        private val builder: PersistentHashMapBuilder<K, V>,
        path: Array<TrieNodeBaseIterator<K, V, T>>
) : MutableIterator<T>, PersistentHashMapBaseIterator<K, V, T>(builder.node, path) {

    private var lastIteratedKey: K? = null
    private var nextWasInvoked = false
    private var expectedModCount = builder.modCount

    override fun next(): T {
        checkForComodification()
        lastIteratedKey = currentKey()
        nextWasInvoked = true
        return super.next()
    }

    override fun remove() {
        checkNextWasInvoked()
        if (hasNext()) {
            val currentKey = currentKey()

            builder.remove(lastIteratedKey)
            resetPath(currentKey.hashCode(), builder.node, currentKey, 0, lastIteratedKey.hashCode(), afterRemove = true)
        } else {
            builder.remove(lastIteratedKey)
        }

        lastIteratedKey = null
        nextWasInvoked = false
        expectedModCount = builder.modCount
    }

    fun setValue(key: K, newValue: V) {
        if (!builder.containsKey(key)) return

        if (hasNext()) {
            val currentKey = currentKey()

            builder[key] = newValue
            resetPath(currentKey.hashCode(), builder.node, currentKey, 0)
        } else {
            builder[key] = newValue
        }

        expectedModCount = builder.modCount
    }

    private fun resetPath(keyHash: Int, node: TrieNode<*, *>, key: K, pathIndex: Int, removedKeyHash: Int = 0, afterRemove: Boolean = false) {
        val shift = pathIndex * LOG_MAX_BRANCHING_FACTOR

        if (shift > MAX_SHIFT) {    // collision
            path[pathIndex].reset(node.buffer, node.buffer.size, 0)
            while (path[pathIndex].currentKey() != key) {
                path[pathIndex].moveToNextKey()
            }
            pathLastIndex = pathIndex
            return
        }

        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (node.hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = node.entryKeyIndex(keyPositionMask)

            // After removing an element, we need to handle node promotion properly to maintain a correct iteration order.
            // `removedKeyPositionMask` represents the bit position of the removed key's hash at the current level.
            // This is needed to detect if the current key was potentially promoted from a deeper level.
            val removedKeyPositionMask = if (afterRemove) 1 shl indexSegment(removedKeyHash, shift) else 0

            // Check if the removed key is at the same position as the current key and was previously at a deeper level.
            // This indicates a node promotion occurred during removal,
            // and we need to handle it in a special way to prevent re-traversing already visited elements.
            if (keyPositionMask == removedKeyPositionMask && pathIndex < pathLastIndex) {
                // Instead of traversing the normal way, we create a special path entry at the previous depth
                // that points directly to the promoted entry, maintaining the original iteration sequence.
                path[pathLastIndex].reset(arrayOf(node.buffer[keyIndex], node.buffer[keyIndex + 1]), ENTRY_SIZE)
                return
            }

//            assert(node.keyAtIndex(keyIndex) == key)

            path[pathIndex].reset(node.buffer, ENTRY_SIZE * node.entryCount(), keyIndex)
            pathLastIndex = pathIndex
            return
        }

//        assert(node.hasNodeAt(keyPositionMask)) // key is in node

        val nodeIndex = node.nodeIndex(keyPositionMask)
        val targetNode = node.nodeAtIndex(nodeIndex)
        path[pathIndex].reset(node.buffer, ENTRY_SIZE * node.entryCount(), nodeIndex)
        resetPath(keyHash, targetNode, key, pathIndex + 1, removedKeyHash, afterRemove)
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.modCount != expectedModCount)
            throw ConcurrentModificationException()
    }
}

internal class PersistentHashMapBuilderEntriesIterator<K, V>(
        builder: PersistentHashMapBuilder<K, V>
) : MutableIterator<MutableMap.MutableEntry<K, V>> {
    private val base = PersistentHashMapBuilderBaseIterator<K, V, MutableMap.MutableEntry<K, V>>(
            builder,
            Array(TRIE_MAX_HEIGHT + 1) { TrieNodeMutableEntriesIterator(this) }
    )

    override fun hasNext(): Boolean = base.hasNext()
    override fun next(): MutableMap.MutableEntry<K, V> = base.next()
    override fun remove(): Unit = base.remove()

    fun setValue(key: K, newValue: V): Unit = base.setValue(key, newValue)
}

internal class PersistentHashMapBuilderKeysIterator<K, V>(builder: PersistentHashMapBuilder<K, V>)
    : PersistentHashMapBuilderBaseIterator<K, V, K>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeKeysIterator<K, V>() })

internal class PersistentHashMapBuilderValuesIterator<K, V>(builder: PersistentHashMapBuilder<K, V>)
    : PersistentHashMapBuilderBaseIterator<K, V, V>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeValuesIterator<K, V>() })