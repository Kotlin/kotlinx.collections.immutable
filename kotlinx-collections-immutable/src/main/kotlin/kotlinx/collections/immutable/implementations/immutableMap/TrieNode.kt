/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.collections.immutable.implementations.immutableMap


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val ENTRY_SIZE = 2
internal const val MAX_SHIFT = 30

/**
 * Gets trie index segment of the specified [index] at the level specified by [shift].
 *
 * `shift` equal to zero corresponds to the root level.
 * For each lower level `shift` increments by [LOG_MAX_BRANCHING_FACTOR].
 */
internal fun indexSegment(index: Int, shift: Int): Int =
        (index shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

private fun <K, V> Array<Any?>.insertEntryAtIndex(keyIndex: Int, key: K, value: V): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size + ENTRY_SIZE)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex + ENTRY_SIZE, startIndex = keyIndex, endIndex = this.size)
    newBuffer[keyIndex] = key
    newBuffer[keyIndex + 1] = value
    return newBuffer
}

private fun Array<Any?>.replaceEntryWithNode(keyIndex: Int, nodeIndex: Int, newNode: TrieNode<*, *>): Array<Any?> {
    val newNodeIndex = nodeIndex - ENTRY_SIZE  // place where to insert new node in the new buffer
    val newBuffer = arrayOfNulls<Any?>(this.size - ENTRY_SIZE + 1)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex, startIndex = keyIndex + ENTRY_SIZE, endIndex = nodeIndex)
    newBuffer[newNodeIndex] = newNode
    this.copyInto(newBuffer, newNodeIndex + 1, startIndex = nodeIndex, endIndex = this.size)
    return newBuffer
}

private fun Array<Any?>.removeEntryAtIndex(keyIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size - ENTRY_SIZE)
    this.copyInto(newBuffer, endIndex = keyIndex)
    this.copyInto(newBuffer, keyIndex, startIndex = keyIndex + ENTRY_SIZE, endIndex = this.size)
    return newBuffer
}

private fun Array<Any?>.removeNodeAtIndex(nodeIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size - 1)
    this.copyInto(newBuffer, endIndex = nodeIndex)
    this.copyInto(newBuffer, nodeIndex, startIndex = nodeIndex + 1, endIndex = this.size)
    return newBuffer
}



internal class TrieNode<K, V>(
        private var dataMap: Int,
        private var nodeMap: Int,
        buffer: Array<Any?>,
        private val marker: Marker?
) {
    constructor(dataMap: Int, nodeMap: Int, buffer: Array<Any?>) : this(dataMap, nodeMap, buffer, null)

    internal var buffer: Array<Any?> = buffer
        private set

    /** Returns number of entries stored in this trie node (not counting subnodes) */
    internal fun entryCount(): Int = Integer.bitCount(dataMap)

    // here and later:
    // positionMask — an int in form 2^n, i.e. having the single bit set, whose ordinal is a logical position in buffer


    /** Returns true if the data bit map has the bit specified by [positionMask] set, indicating there's a data entry in the buffer at that position. */
    internal fun hasEntryAt(positionMask: Int): Boolean {
        return dataMap and positionMask != 0
    }

    /** Returns true if the node bit map has the bit specified by [positionMask] set, indicating there's a subtrie node in the buffer at that position. */
    private fun hasNodeAt(positionMask: Int): Boolean {
        return nodeMap and positionMask != 0
    }

    /** Gets the index in buffer of the data entry key corresponding to the position specified by [positionMask]. */
    internal fun entryKeyIndex(positionMask: Int): Int {
        return ENTRY_SIZE * Integer.bitCount(dataMap and (positionMask - 1))
    }

    /** Gets the index in buffer of the subtrie node entry corresponding to the position specified by [positionMask]. */
    internal fun nodeIndex(positionMask: Int): Int {
        return buffer.size - 1 - Integer.bitCount(nodeMap and (positionMask - 1))
    }

    /** Retrieves the buffer element at the given [keyIndex] as key of a data entry. */
    private fun keyAtIndex(keyIndex: Int): K {
        @Suppress("UNCHECKED_CAST")
        return buffer[keyIndex] as K
    }

    /** Retrieves the buffer element next to the given [keyIndex] as value of a data entry. */
    private fun valueAtKeyIndex(keyIndex: Int): V {
        @Suppress("UNCHECKED_CAST")
        return buffer[keyIndex + 1] as V
    }

    /** Retrieves the buffer element at the given [nodeIndex] as subtrie node. */
    internal fun nodeAtIndex(nodeIndex: Int): TrieNode<K, V> {
        @Suppress("UNCHECKED_CAST")
        return buffer[nodeIndex] as TrieNode<K, V>
    }

    private fun insertEntryAt(positionMask: Int, key: K, value: V): TrieNode<K, V> {
//        assert(!hasEntryAt(positionMask))

        val keyIndex = entryKeyIndex(positionMask)
        val newBuffer = buffer.insertEntryAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or positionMask, nodeMap, newBuffer)
    }

    private fun mutableInsertEntryAt(positionMask: Int, key: K, value: V, mutatorMarker: Marker): TrieNode<K, V> {
//        assert(!hasEntryAt(positionMask))

        val keyIndex = entryKeyIndex(positionMask)
        if (marker === mutatorMarker) {
            buffer = buffer.insertEntryAtIndex(keyIndex, key, value)
            dataMap = dataMap or positionMask
            return this
        }
        val newBuffer = buffer.insertEntryAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or positionMask, nodeMap, newBuffer, mutatorMarker)
    }

    private fun updateValueAtIndex(keyIndex: Int, value: V): TrieNode<K, V> {
//        assert(buffer[keyIndex + 1] !== value)

        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateValueAtIndex(keyIndex: Int, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
//        assert(buffer[keyIndex + 1] !== value)

        // If the [mutator] is exclusive owner of this node, update value at specified index in-place.
        if (marker === mutator.marker) {
            buffer[keyIndex + 1] = value
            return this
        }
        // Structural change due to node replacement.
        mutator.modCount++
        // Create new node with updated value at specified index.
        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer, mutator.marker)
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>, mutatorMarker: Marker): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        if (marker === mutatorMarker) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer, mutatorMarker)
    }

    private fun bufferMoveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                      newKey: K, newValue: V, shift: Int, mutatorMarker: Marker?): Array<Any?> {
        val storedKey = keyAtIndex(keyIndex)
        val storedKeyHash = storedKey.hashCode()
        val storedValue = valueAtKeyIndex(keyIndex)
        val newNode = makeNode(storedKeyHash, storedKey, storedValue,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)

        val nodeIndex = nodeIndex(positionMask) + 1 // place where to insert new node in the current buffer

        return buffer.replaceEntryWithNode(keyIndex, nodeIndex, newNode)
    }


    private fun moveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                newKey: K, newValue: V, shift: Int): TrieNode<K, V> {
//        assert(hasEntryAt(positionMask))
//        assert(!hasNodeAt(positionMask))

        val newBuffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, null)
        return TrieNode(dataMap xor positionMask, nodeMap or positionMask, newBuffer)
    }

    private fun mutableMoveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                       newKey: K, newValue: V, shift: Int, mutatorMarker: Marker): TrieNode<K, V> {
//        assert(hasEntryAt(positionMask))
//        assert(!hasNodeAt(positionMask))

        if (marker === mutatorMarker) {
            buffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, mutatorMarker)
            dataMap = dataMap xor positionMask
            nodeMap = nodeMap or positionMask
            return this
        }
        val newBuffer = bufferMoveEntryToNode(keyIndex, positionMask, newKeyHash, newKey, newValue, shift, mutatorMarker)
        return TrieNode(dataMap xor positionMask, nodeMap or positionMask, newBuffer, mutatorMarker)
    }

    /** Creates a new TrieNode for holding two given key value entries */
    private fun makeNode(keyHash1: Int, key1: K, value1: V,
                         keyHash2: Int, key2: K, value2: V, shift: Int, mutatorMarker: Marker?): TrieNode<K, V> {
        if (shift > MAX_SHIFT) {
//            assert(key1 != key2)
            // when two key hashes are entirely equal: the last level subtrie node stores them just as unordered list
            return TrieNode(0, 0, arrayOf(key1, value1, key2, value2), mutatorMarker)
        }

        val setBit1 = indexSegment(keyHash1, shift)
        val setBit2 = indexSegment(keyHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf(key1, value1, key2, value2)
            } else {
                arrayOf(key2, value2, key1, value1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), 0, nodeBuffer, mutatorMarker)
        }
        // hash segments at the given shift are equal: move these entries into the subtrie
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
        return TrieNode(0, 1 shl setBit1, arrayOf<Any?>(node), mutatorMarker)
    }

    private fun removeEntryAtIndex(keyIndex: Int, positionMask: Int): TrieNode<K, V>? {
//        assert(hasEntryAt(positionMask))
        if (buffer.size == ENTRY_SIZE) return null

        val newBuffer = buffer.removeEntryAtIndex(keyIndex)
        return TrieNode(dataMap xor positionMask, nodeMap, newBuffer)
    }

    private fun mutableRemoveEntryAtIndex(keyIndex: Int, positionMask: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
//        assert(hasEntryAt(positionMask))
        mutator.size--
        mutator.operationResult = buffer[keyIndex + 1] as V
        if (buffer.size == ENTRY_SIZE) return null

        if (marker === mutator.marker) {
            buffer = buffer.removeEntryAtIndex(keyIndex)
            dataMap = dataMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(keyIndex)
        return TrieNode(dataMap xor positionMask, nodeMap, newBuffer, mutator.marker)
    }

    private fun collisionRemoveEntryAtIndex(i: Int): TrieNode<K, V>? {
        if (buffer.size == ENTRY_SIZE) return null

        val newBuffer = buffer.removeEntryAtIndex(i)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionRemoveEntryAtIndex(i: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        mutator.size--
        @Suppress("UNCHECKED_CAST")
        mutator.operationResult = buffer[i + 1] as V
        if (buffer.size == ENTRY_SIZE) return null

        if (marker === mutator.marker) {
            buffer = buffer.removeEntryAtIndex(i)
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(i)
        return TrieNode(0, 0, newBuffer, mutator.marker)
    }

    private fun removeNodeAtIndex(nodeIndex: Int, positionMask: Int): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer)
    }

    private fun mutableRemoveNodeAtIndex(nodeIndex: Int, positionMask: Int, mutatorMarker: Marker): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        if (marker === mutatorMarker) {
            buffer = buffer.removeNodeAtIndex(nodeIndex)
            nodeMap = nodeMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer, mutatorMarker)
    }

    private fun collisionContainsKey(key: K): Boolean {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) return true
        }
        return false
    }

    private fun collisionGet(key: K): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                @Suppress("UNCHECKED_CAST")
                return buffer[i + 1] as V
            }
        }
        return null
    }

    private fun collisionPut(key: K, value: V, modification: ModificationWrapper): TrieNode<K, V> {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                if (value === buffer[i + 1]) {
                    return this
                }
                modification.value = UPDATE_VALUE
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return TrieNode(0, 0, newBuffer)
            }
        }
        modification.value = PUT_KEY_VALUE
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionPut(key: K, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        // Check if there is an entry with the specified key.
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) { // found entry with the specified key
                @Suppress("UNCHECKED_CAST")
                mutator.operationResult = buffer[i + 1] as V

                // If the [mutator] is exclusive owner of this node, update value of the entry in-place.
                if (marker === mutator.marker) {
                    buffer[i + 1] = value
                    return this
                }

                // Structural change due to node replacement.
                mutator.modCount++
                // Create new node with updated entry value.
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return TrieNode(0, 0, newBuffer, mutator.marker)
            }
        }
        // Create new collision node with the specified entry added to it.
        mutator.size++
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return TrieNode(0, 0, newBuffer, mutator.marker)
    }

    private fun collisionRemove(key: K): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                return mutableCollisionRemoveEntryAtIndex(i, mutator)
            }
        }
        return this
    }

    private fun collisionRemove(key: K, value: V): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i] && value == buffer[i + 1]) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i] && value == buffer[i + 1]) {
                return mutableCollisionRemoveEntryAtIndex(i, mutator)
            }
        }
        return this
    }

    fun containsKey(keyHash: Int, key: K, shift: Int): Boolean {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            return key == keyAtIndex(entryKeyIndex(keyPositionMask))
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val targetNode = nodeAtIndex(nodeIndex(keyPositionMask))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionContainsKey(key)
            }
            return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return false
    }

    fun get(keyHash: Int, key: K, shift: Int): V? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return valueAtKeyIndex(keyIndex)
            }
            return null
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val targetNode = nodeAtIndex(nodeIndex(keyPositionMask))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionGet(key)
            }
            return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return null
    }

    fun put(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, modification: ModificationWrapper): TrieNode<K, V> {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                modification.value = UPDATE_VALUE
                if (valueAtKeyIndex(keyIndex) === value) return this

                return updateValueAtIndex(keyIndex, value)
            }
            modification.value = PUT_KEY_VALUE
            return moveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift)
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionPut(key, value, modification)
            } else {
                targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            if (targetNode === newNode) return this
            return updateNodeAtIndex(nodeIndex, newNode)
        }

        // no entry at this key hash segment
        modification.value = PUT_KEY_VALUE
        return insertEntryAt(keyPositionMask, key, value)
    }

    fun mutablePut(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                mutator.operationResult = valueAtKeyIndex(keyIndex)
                if (valueAtKeyIndex(keyIndex) === value) {
                    return this
                }

                return mutableUpdateValueAtIndex(keyIndex, value, mutator)
            }
            mutator.size++
            return mutableMoveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift, mutator.marker)
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionPut(key, value, mutator)
            } else {
                targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode === newNode) {
                return this
            }
            return mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.marker)
        }

        // key is absent
        mutator.size++
        return mutableInsertEntryAt(keyPositionMask, key, value, mutator.marker)
    }

    fun remove(keyHash: Int, key: K, shift: Int): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return removeEntryAtIndex(keyIndex, keyPositionMask)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key)
            } else {
                targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            return when {
                targetNode === newNode -> this
                newNode == null -> removeNodeAtIndex(nodeIndex, keyPositionMask)
                else -> updateNodeAtIndex(nodeIndex, newNode)
            }
        }

        // key is absent
        return this
    }

    fun mutableRemove(keyHash: Int, key: K, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveNodeAtIndex(nodeIndex, keyPositionMask, mutator.marker)
                else -> mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.marker)
            }
        }

        // key is absent
        return this
    }

    fun remove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex) && value == valueAtKeyIndex(keyIndex)) {
                return removeEntryAtIndex(keyIndex, keyPositionMask)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, value)
            } else {
                targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            return when {
                targetNode === newNode -> this
                newNode == null -> removeNodeAtIndex(nodeIndex, keyPositionMask)
                else -> updateNodeAtIndex(nodeIndex, newNode)
            }
        }

        // key is absent
        return this
    }

    fun mutableRemove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex) && value == valueAtKeyIndex(keyIndex)) {
                return mutableRemoveEntryAtIndex(keyIndex, keyPositionMask, mutator)
            }
            return this
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, value, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveNodeAtIndex(nodeIndex, keyPositionMask, mutator.marker)
                else -> mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.marker)
            }
        }

        // key is absent
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing, Nothing>(0, 0, emptyArray())
    }
}