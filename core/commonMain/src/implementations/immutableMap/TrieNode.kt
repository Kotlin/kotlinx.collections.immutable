/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.internal.MutabilityOwnership


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val ENTRY_SIZE = 2

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
        private val ownedBy: MutabilityOwnership?
) {
    constructor(dataMap: Int, nodeMap: Int, buffer: Array<Any?>) : this(dataMap, nodeMap, buffer, null)

    internal class ModificationResult<K, V>(var node: TrieNode<K, V>, val sizeDelta: Int) {
        inline fun replaceNode(operation: (TrieNode<K, V>) -> TrieNode<K, V>): ModificationResult<K, V> =
                apply { node = operation(node) }
    }

    private fun asInsertResult() = ModificationResult(this, 1)
    private fun asUpdateResult() = ModificationResult(this, 0)

    internal var buffer: Array<Any?> = buffer
        private set

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun isCollision(): Boolean {
        return dataMap == 0 && nodeMap == 0 && buffer.isNotEmpty()
    }

    /** Returns number of entries stored in this trie node (not counting subnodes) */
    @UseExperimental(ExperimentalStdlibApi::class)
    internal fun entryCount(): Int = dataMap.countOneBits()

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
    @UseExperimental(ExperimentalStdlibApi::class)
    internal fun entryKeyIndex(positionMask: Int): Int {
        return ENTRY_SIZE * (dataMap and (positionMask - 1)).countOneBits()
    }

    /** Gets the index in buffer of the subtrie node entry corresponding to the position specified by [positionMask]. */
    @UseExperimental(ExperimentalStdlibApi::class)
    internal fun nodeIndex(positionMask: Int): Int {
        return buffer.size - 1 - (nodeMap and (positionMask - 1)).countOneBits()
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

    private fun mutableInsertEntryAt(positionMask: Int, key: K, value: V, owner: MutabilityOwnership): TrieNode<K, V> {
//        assert(!hasEntryAt(positionMask))

        val keyIndex = entryKeyIndex(positionMask)
        if (ownedBy === owner) {
            buffer = buffer.insertEntryAtIndex(keyIndex, key, value)
            dataMap = dataMap or positionMask
            return this
        }
        val newBuffer = buffer.insertEntryAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or positionMask, nodeMap, newBuffer, owner)
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
        if (ownedBy === mutator.ownership) {
            buffer[keyIndex + 1] = value
            return this
        }
        // Structural change due to node replacement.
        mutator.modCount++
        // Create new node with updated value at specified index.
        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer, mutator.ownership)
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>, owner: MutabilityOwnership): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        if (ownedBy === owner) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer, owner)
    }

    private fun moveEntryToNode(keyIndex: Int, positionMask: Int, newKeyHash: Int,
                                      newKey: K, newValue: V, shift: Int, owner: MutabilityOwnership?): TrieNode<K, V> {
        val storedKey = keyAtIndex(keyIndex)
        val storedKeyHash = storedKey.hashCode()
        val storedValue = valueAtKeyIndex(keyIndex)

        val newNode: TrieNode<K, V>
        if (storedKeyHash == newKeyHash) {
            newNode = makeCollisionNode(newKey, newValue, storedKey, storedValue, owner)
            if (buffer.size == ENTRY_SIZE) {
                return newNode
            }
        } else {
            newNode = makeNode(storedKeyHash, storedKey, storedValue,
                    newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        }

        val nodeIndex = nodeIndex(positionMask) + 1 // place where to insert new node in the current buffer

        val newBuffer = buffer.replaceEntryWithNode(keyIndex, nodeIndex, newNode)
        val newDataMap = dataMap xor positionMask
        val newNodeMap = nodeMap or positionMask

        if (ownedBy != null && ownedBy === owner) {
            buffer = newBuffer
            dataMap = newDataMap
            nodeMap = newNodeMap
            return this
        }

        return TrieNode(newDataMap, newNodeMap, newBuffer, owner)
    }

    private fun makeCollisionNode(key1: K, value1: V, key2: K, value2: V, owner: MutabilityOwnership?): TrieNode<K, V> {
        return makeCollisionNode(arrayOf(key1, value1, key2, value2), owner)
    }

    private fun makeCollisionNode(buffer: Array<Any?>, owner: MutabilityOwnership?): TrieNode<K, V> {
        return TrieNode(0, 0, buffer, owner)
    }

    /** Creates a new TrieNode for holding two given key value entries */
    private fun makeNode(keyHash1: Int, key1: K, value1: V,
                         keyHash2: Int, key2: K, value2: V, shift: Int, owner: MutabilityOwnership?): TrieNode<K, V> {
//        assert(keyHash1 != keyHash2)

        val setBit1 = indexSegment(keyHash1, shift)
        val setBit2 = indexSegment(keyHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf(key1, value1, key2, value2)
            } else {
                arrayOf(key2, value2, key1, value1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), 0, nodeBuffer, owner)
        }
        // hash segments at the given shift are equal: move these entries into the subtrie
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return TrieNode(0, 1 shl setBit1, arrayOf<Any?>(node), owner)
    }

    /** Creates a new TrieNode for holding the given key value entry and the given collision node */
    private fun makeNode(collisionHash: Int, collisionNode: TrieNode<K, V>,
                         keyHash: Int, key: K, value: V, shift: Int, owner: MutabilityOwnership?): TrieNode<K, V> {
//        assert(collisionHash != keyHash)

        val collisionBits = indexSegment(collisionHash, shift)
        val keyBits = indexSegment(keyHash, shift)

        if (collisionBits != keyBits) {
            val nodeBuffer = arrayOf(key, value, collisionNode)
            return TrieNode(1 shl keyBits, (1 shl collisionBits), nodeBuffer, owner)
        }

        val node = makeNode(collisionHash, collisionNode, keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return TrieNode(0, 1 shl collisionBits, arrayOf<Any?>(node), owner)
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
        mutator.operationResult = valueAtKeyIndex(keyIndex)
        if (buffer.size == ENTRY_SIZE) return null

        if (ownedBy === mutator.ownership) {
            buffer = buffer.removeEntryAtIndex(keyIndex)
            dataMap = dataMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(keyIndex)
        return TrieNode(dataMap xor positionMask, nodeMap, newBuffer, mutator.ownership)
    }

    private fun collisionRemoveEntryAtIndex(i: Int): TrieNode<K, V>? {
        if (buffer.size == ENTRY_SIZE) return null

        val newBuffer = buffer.removeEntryAtIndex(i)
        return makeCollisionNode(newBuffer, null)
    }

    private fun mutableCollisionRemoveEntryAtIndex(i: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        mutator.size--
        mutator.operationResult = valueAtKeyIndex(i)
        if (buffer.size == ENTRY_SIZE) return null

        if (ownedBy === mutator.ownership) {
            buffer = buffer.removeEntryAtIndex(i)
            return this
        }
        val newBuffer = buffer.removeEntryAtIndex(i)
        return makeCollisionNode(newBuffer, mutator.ownership)
    }

    private fun removeNodeAtIndex(nodeIndex: Int, positionMask: Int): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer)
    }

    private fun mutableRemoveNodeAtIndex(nodeIndex: Int, positionMask: Int, owner: MutabilityOwnership): TrieNode<K, V>? {
//        assert(hasNodeAt(positionMask))
        if (buffer.size == 1) return null

        if (ownedBy === owner) {
            buffer = buffer.removeNodeAtIndex(nodeIndex)
            nodeMap = nodeMap xor positionMask
            return this
        }
        val newBuffer = buffer.removeNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor positionMask, newBuffer, owner)
    }

    private fun collisionContainsKey(keyHash: Int, key: K): Boolean {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return false
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) return true
        }
        return false
    }

    private fun collisionGet(keyHash: Int, key: K): V? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return null
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return valueAtKeyIndex(i)
            }
        }
        return null
    }

    private fun collisionPut(keyHash: Int, key: K, value: V, shift: Int): ModificationResult<K, V>? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return makeNode(collisionHash, this, keyHash, key, value, shift, null).asInsertResult()
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                if (value === valueAtKeyIndex(i)) {
                    return null
                }
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return makeCollisionNode(newBuffer, null).asUpdateResult()
            }
        }
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return makeCollisionNode(newBuffer, null).asInsertResult()
    }

    private fun mutableCollisionPut(keyHash: Int, key: K, value: V, shift: Int, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V> {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            mutator.size++
            return makeNode(collisionHash, this, keyHash, key, value, shift, mutator.ownership)
        }

        // Check if there is an entry with the specified key.
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) { // found entry with the specified key
                mutator.operationResult = valueAtKeyIndex(i)

                // If the [mutator] is exclusive owner of this node, update value of the entry in-place.
                if (ownedBy === mutator.ownership) {
                    buffer[i + 1] = value
                    return this
                }

                // Structural change due to node replacement.
                mutator.modCount++
                // Create new node with updated entry value.
                val newBuffer = buffer.copyOf()
                newBuffer[i + 1] = value
                return makeCollisionNode(newBuffer, mutator.ownership)
            }
        }
        // Create new collision node with the specified entry added to it.
        mutator.size++
        val newBuffer = buffer.insertEntryAtIndex(0, key, value)
        return makeCollisionNode(newBuffer, mutator.ownership)
    }

    private fun collisionRemove(keyHash: Int, key: K): TrieNode<K, V>? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return this
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(keyHash: Int, key: K, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return this
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i)) {
                return mutableCollisionRemoveEntryAtIndex(i, mutator)
            }
        }
        return this
    }

    private fun collisionRemove(keyHash: Int, key: K, value: V): TrieNode<K, V>? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return this
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i) && value == valueAtKeyIndex(i)) {
                return collisionRemoveEntryAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(keyHash: Int, key: K, value: V, mutator: PersistentHashMapBuilder<K, V>): TrieNode<K, V>? {
        val collisionHash = keyAtIndex(0).hashCode()
        if (keyHash != collisionHash) {
            return this
        }

        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == keyAtIndex(i) && value == valueAtKeyIndex(i)) {
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
            return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        if (isCollision()) {
            return collisionContainsKey(keyHash, key)
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
            return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        if (isCollision()) {
            return collisionGet(keyHash, key)
        }

        // key is absent
        return null
    }

    fun put(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): ModificationResult<K, V>? {
        val keyPositionMask = 1 shl indexSegment(keyHash, shift)

        if (hasEntryAt(keyPositionMask)) { // key is directly in buffer
            val keyIndex = entryKeyIndex(keyPositionMask)

            if (key == keyAtIndex(keyIndex)) {
                if (valueAtKeyIndex(keyIndex) === value) return null

                return updateValueAtIndex(keyIndex, value).asUpdateResult()
            }
            return moveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift, null).asInsertResult()
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val putResult = targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR) ?: return null
            return putResult.replaceNode { node -> updateNodeAtIndex(nodeIndex, node) }
        }

        if (isCollision()) {
            return collisionPut(keyHash, key, value, shift)
        }

        // no entry at this key hash segment
        return insertEntryAt(keyPositionMask, key, value).asInsertResult()
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
            return moveEntryToNode(keyIndex, keyPositionMask, keyHash, key, value, shift, mutator.ownership)
        }
        if (hasNodeAt(keyPositionMask)) { // key is in node
            val nodeIndex = nodeIndex(keyPositionMask)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            if (targetNode === newNode) {
                return this
            }
            return mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.ownership)
        }

        if (isCollision()) {
            return mutableCollisionPut(keyHash, key, value, shift, mutator)
        }

        // key is absent
        mutator.size++
        return mutableInsertEntryAt(keyPositionMask, key, value, mutator.ownership)
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
            val newNode = targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
            return when {
                targetNode === newNode -> this
                newNode == null -> removeNodeAtIndex(nodeIndex, keyPositionMask)
                else -> updateNodeAtIndex(nodeIndex, newNode)
            }
        }

        if (isCollision()) {
            return collisionRemove(keyHash, key)
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
            val newNode = targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveNodeAtIndex(nodeIndex, keyPositionMask, mutator.ownership)
                else -> mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.ownership)
            }
        }

        if (isCollision()) {
            return mutableCollisionRemove(keyHash, key, mutator)
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
            val newNode = targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
            return when {
                targetNode === newNode -> this
                newNode == null -> removeNodeAtIndex(nodeIndex, keyPositionMask)
                else -> updateNodeAtIndex(nodeIndex, newNode)
            }
        }

        if (isCollision()) {
            return collisionRemove(keyHash, key, value)
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
            val newNode = targetNode.mutableRemove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveNodeAtIndex(nodeIndex, keyPositionMask, mutator.ownership)
                else -> mutableUpdateNodeAtIndex(nodeIndex, newNode, mutator.ownership)
            }
        }

        if (isCollision()) {
            return mutableCollisionRemove(keyHash, key, value, mutator)
        }

        // key is absent
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing, Nothing>(0, 0, emptyArray())
    }
}