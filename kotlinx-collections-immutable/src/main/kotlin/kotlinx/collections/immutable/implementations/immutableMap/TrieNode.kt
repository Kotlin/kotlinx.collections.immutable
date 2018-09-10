package kotlinx.collections.immutable.implementations.immutableMap


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val ENTRY_SIZE = 2
internal const val MAX_SHIFT = 30
internal const val NULL_HASH_CODE = 0


internal class TrieNode<K, V>(var dataMap: Int,
                              var nodeMap: Int,
                              var buffer: Array<Any?>,
                              var marker: Marker?) {

    constructor(dataMap: Int, nodeMap: Int, buffer: Array<Any?>) : this(dataMap, nodeMap, buffer, null)

    fun makeMutableFor(mutator: PersistentHashMapBuilder<*, *>): TrieNode<K, V> {
        if (marker === mutator.marker) { return this }
        return TrieNode(dataMap, nodeMap, buffer.copyOf(), mutator.marker)
    }

    private fun ensureMutableBy(mutator: PersistentHashMapBuilder<*, *>) {
        if (marker !== mutator.marker) {
            throw IllegalStateException("Markers expected to be same")
        }
    }

    internal fun hasDataAt(position: Int): Boolean {
        return dataMap and position != 0
    }

    internal fun hasNodeAt(position: Int): Boolean {
        return nodeMap and position != 0
    }

    internal fun keyDataIndex(position: Int): Int {
        return ENTRY_SIZE * Integer.bitCount(dataMap and (position - 1))
    }

    internal fun keyNodeIndex(position: Int): Int {
        return buffer.size - 1 - Integer.bitCount(nodeMap and (position - 1))
    }

    internal fun keyAtIndex(keyIndex: Int): K {
        return buffer[keyIndex] as K
    }

    private fun valueAtKeyIndex(keyIndex: Int): V {
        return buffer[keyIndex + 1] as V
    }

    internal fun nodeAtIndex(nodeIndex: Int): TrieNode<K, V> {
        return buffer[nodeIndex] as TrieNode<K, V>
    }

    private fun bufferPutDataAtIndex(keyIndex: Int, key: K, value: V): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex, newBuffer, keyIndex + 2, buffer.size - keyIndex)
        newBuffer[keyIndex] = key
        newBuffer[keyIndex + 1] = value
        return newBuffer
    }

    private fun putDataAt(position: Int, key: K, value: V): TrieNode<K, V> {
//        assert(!hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        val newBuffer = bufferPutDataAtIndex(keyIndex, key, value)
        return TrieNode(dataMap or position, nodeMap, newBuffer)
    }

    private fun mutablePutDataAt(position: Int, key: K, value: V) {
//        assert(!hasDataAt(position))

        val keyIndex = keyDataIndex(position)
        buffer = bufferPutDataAtIndex(keyIndex, key, value)
        dataMap = dataMap or position
    }

    private fun updateValueAtIndex(keyIndex: Int, value: V): TrieNode<K, V> {
//        assert(buffer[keyIndex + 1] !== value)

        val newBuffer = buffer.copyOf()
        newBuffer[keyIndex + 1] = value
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateValueAtIndex(keyIndex: Int, value: V): V {
        val previousValue = buffer[keyIndex + 1]
        buffer[keyIndex + 1] = value
        return previousValue as V
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>): TrieNode<K, V> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(dataMap, nodeMap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<K, V>) {
        buffer[nodeIndex] = newNode
    }

    private fun bufferMoveDataToNode(keyIndex: Int, position: Int, newKeyHash: Int,
                                     newKey: K, newValue: V, shift: Int, mutatorMarker: Marker?): Array<Any?> {
        val storedKey = keyAtIndex(keyIndex)
        val storedKeyHash = storedKey?.hashCode() ?: NULL_HASH_CODE
        val storedValue = valueAtKeyIndex(keyIndex)
        val newNode = makeNode(storedKeyHash, storedKey, storedValue,
                newKeyHash, newKey, newValue, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)

        val nodeIndex = keyNodeIndex(position) - 1
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, nodeIndex - keyIndex)
        System.arraycopy(buffer, nodeIndex + 2, newBuffer, nodeIndex + 1, buffer.size - nodeIndex - 2)

        newBuffer[nodeIndex] = newNode
        return newBuffer
    }

    private fun moveDataToNode(keyIndex: Int, position: Int, newKeyHash: Int,
                               newKey: K, newValue: V, shift: Int): TrieNode<K, V> {
//        assert(hasDataAt(position))
//        assert(!hasNodeAt(position))

        val newBuffer = bufferMoveDataToNode(keyIndex, position, newKeyHash, newKey, newValue, shift, null)
        return TrieNode(dataMap xor position, nodeMap or position, newBuffer)
    }

    private fun mutableMoveDataToNode(keyIndex: Int, position: Int, newKeyHash: Int,
                                      newKey: K, newValue: V, shift: Int, mutator: PersistentHashMapBuilder<*, *>) {
//        assert(hasDataAt(position))
//        assert(!hasNodeAt(position))

        buffer = bufferMoveDataToNode(keyIndex, position, newKeyHash, newKey, newValue, shift, mutator.marker)
        dataMap = dataMap xor position
        nodeMap = nodeMap or position
    }

    private fun makeNode(keyHash1: Int, key1: K, value1: V,
                         keyHash2: Int, key2: K, value2: V, shift: Int, mutatorMarker: Marker?): TrieNode<K, V> {
        if (shift > MAX_SHIFT) {
//            assert(key1 != key2)
            return TrieNode(0, 0, arrayOf(key1, value1, key2, value2), mutatorMarker)
        }

        val setBit1 = (keyHash1 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
        val setBit2 = (keyHash2 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

        if (setBit1 != setBit2) {
            val nodeBuffer =  if (setBit1 < setBit2) {
                arrayOf(key1, value1, key2, value2)
            } else {
                arrayOf(key2, value2, key1, value1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), 0, nodeBuffer, mutatorMarker)
        }
        val node = makeNode(keyHash1, key1, value1, keyHash2, key2, value2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
        return TrieNode(0, 1 shl setBit1, arrayOf<Any?>(node), mutatorMarker)
    }

    private fun bufferRemoveDataAtIndex(keyIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 2)
        System.arraycopy(buffer, 0, newBuffer, 0, keyIndex)
        System.arraycopy(buffer, keyIndex + 2, newBuffer, keyIndex, buffer.size - keyIndex - 2)
        return newBuffer
    }

    private fun removeDataAtIndex(keyIndex: Int, position: Int): TrieNode<K, V>? {
//        assert(hasDataAt(position))
        if (buffer.size == 2) { return null }

        val newBuffer = bufferRemoveDataAtIndex(keyIndex)
        return TrieNode(dataMap xor position, nodeMap, newBuffer)
    }

    private fun mutableRemoveDataAtIndex(keyIndex: Int, position: Int): V {
//        assert(hasDataAt(position))
        val previousValue = buffer[keyIndex + 1]
        buffer = bufferRemoveDataAtIndex(keyIndex)
        dataMap = dataMap xor position
        return previousValue as V
    }

    private fun collisionRemoveDataAtIndex(i: Int): TrieNode<K, V>? {
        if (buffer.size == 2) { return null }

        val newBuffer = bufferRemoveDataAtIndex(i)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionRemoveDataAtIndex(i: Int): V? {
        val previousValue = buffer[i + 1]
        buffer = bufferRemoveDataAtIndex(i)
        return previousValue as V
    }

    private fun bufferRemoveNodeAtIndex(nodeIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, nodeIndex)
        System.arraycopy(buffer, nodeIndex + 1, newBuffer, nodeIndex, buffer.size - nodeIndex - 1)
        return newBuffer
    }

    private fun removeNodeAtIndex(nodeIndex: Int, position: Int): TrieNode<K, V>? {
//        assert(hasNodeAt(position))
        if (buffer.size == 1) { return null }

        val newBuffer = bufferRemoveNodeAtIndex(nodeIndex)
        return TrieNode(dataMap, nodeMap xor position, newBuffer)
    }

    private fun mutableRemoveNodeAtIndex(nodeIndex: Int, position: Int) {
//        assert(hasNodeAt(position))
        buffer = bufferRemoveNodeAtIndex(nodeIndex)
        nodeMap = nodeMap xor position
    }

    private fun collisionContainsKey(key: K): Boolean {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) { return true }
        }
        return false
    }

    private fun collisionGet(key: K): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) { return buffer[i + 1] as V }
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
        val newBuffer = bufferPutDataAtIndex(0, key, value)
        return TrieNode(0, 0, newBuffer)
    }

    private fun mutableCollisionPut(key: K, value: V, mutator: PersistentHashMapBuilder<*, *>): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                val previousValue = buffer[i + 1]
                buffer[i + 1] = value
                return previousValue as V
            }
        }
        mutator.size++
        buffer = bufferPutDataAtIndex(0, key, value)
        return null
    }

    private fun collisionRemove(key: K): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                return collisionRemoveDataAtIndex(i)
            }
        }
        return this
    }

    private fun mutableCollisionRemove(key: K, mutator: PersistentHashMapBuilder<*, *>): V? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i]) {
                mutator.size--
                return mutableCollisionRemoveDataAtIndex(i)
            }
        }
        return null
    }

    private fun collisionRemove(key: K, value: V): TrieNode<K, V>? {
        for (i in 0 until buffer.size step ENTRY_SIZE) {
            if (key == buffer[i] && value == buffer[i + 1]) {
                return collisionRemoveDataAtIndex(i)
            }
        }
        return this
    }

    fun containsKey(keyHash: Int, key: K, shift: Int): Boolean {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            return key == keyAtIndex(keyDataIndex(keyPosition))
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAtIndex(keyNodeIndex(keyPosition))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionContainsKey(key)
            }
            return targetNode.containsKey(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return false
    }

    fun get(keyHash: Int, key: K, shift: Int): V? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex)) {
                return valueAtKeyIndex(keyIndex)
            }
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val targetNode = nodeAtIndex(keyNodeIndex(keyPosition))
            if (shift == MAX_SHIFT) {
                return targetNode.collisionGet(key)
            }
            return targetNode.get(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
        }

        // key is absent
        return null
    }

    fun put(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, modification: ModificationWrapper): TrieNode<K, V> {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex)) {
                if (valueAtKeyIndex(keyIndex) === value) { return this }

                modification.value = UPDATE_VALUE
                return updateValueAtIndex(keyIndex, value)
            }
            modification.value = PUT_KEY_VALUE
            return moveDataToNode(keyIndex, keyPosition, keyHash, key, value, shift)
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val nodeIndex = keyNodeIndex(keyPosition)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionPut(key, value, modification)
            } else {
                targetNode.put(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, modification)
            }
            if (targetNode === newNode) { return this }
            return updateNodeAtIndex(nodeIndex, newNode)
        }

        // key is absent
        modification.value = PUT_KEY_VALUE
        return putDataAt(keyPosition, key, value)
    }

    fun mutablePut(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int, mutator: PersistentHashMapBuilder<*, *>): V? {
        ensureMutableBy(mutator)
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex)) {
                return mutableUpdateValueAtIndex(keyIndex, value)
            }
            mutator.size++
            mutableMoveDataToNode(keyIndex, keyPosition, keyHash, key, value, shift, mutator)
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val nodeIndex = keyNodeIndex(keyPosition)

            val targetNode = nodeAtIndex(nodeIndex).makeMutableFor(mutator)
            mutableUpdateNodeAtIndex(nodeIndex, targetNode)
            return if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionPut(key, value, mutator)
            } else {
                targetNode.mutablePut(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
        }

        // key is absent
        mutator.size++
        mutablePutDataAt(keyPosition, key, value)
        return null
    }

    fun remove(keyHash: Int, key: K, shift: Int): TrieNode<K, V>? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex)) {
                return removeDataAtIndex(keyIndex, keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val nodeIndex = keyNodeIndex(keyPosition)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key)
            } else {
                targetNode.remove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeNodeAtIndex(nodeIndex, keyPosition) }
            return updateNodeAtIndex(nodeIndex, newNode)
        }

        // key is absent
        return this
    }

    fun mutableRemove(keyHash: Int, key: K, shift: Int, mutator: PersistentHashMapBuilder<*, *>): V? {
        ensureMutableBy(mutator)
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex)) {
                mutator.size--
                return mutableRemoveDataAtIndex(keyIndex, keyPosition)
            }
            return null
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val nodeIndex = keyNodeIndex(keyPosition)

            val targetNode = nodeAtIndex(nodeIndex).makeMutableFor(mutator)
            mutableUpdateNodeAtIndex(nodeIndex, targetNode)
            val previousValue = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(key, mutator)
            } else {
                targetNode.mutableRemove(keyHash, key, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode.buffer.isEmpty()) { mutableRemoveNodeAtIndex(nodeIndex, keyPosition) }
            return previousValue
        }

        // key is absent
        return null
    }

    fun remove(keyHash: Int, key: K, value: @UnsafeVariance V, shift: Int): TrieNode<K, V>? {
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = keyDataIndex(keyPosition)

            if (key == keyAtIndex(keyIndex) && value == valueAtKeyIndex(keyIndex)) {
                return removeDataAtIndex(keyIndex, keyPosition)
            }
            return this
        }
        if (hasNodeAt(keyPosition)) { // key is in node
            val nodeIndex = keyNodeIndex(keyPosition)

            val targetNode = nodeAtIndex(nodeIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(key, value)
            } else {
                targetNode.remove(keyHash, key, value, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeNodeAtIndex(nodeIndex, keyPosition) }
            return updateNodeAtIndex(nodeIndex, newNode)
        }

        // key is absent
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing, Nothing>(0, 0, emptyArray())
    }
}