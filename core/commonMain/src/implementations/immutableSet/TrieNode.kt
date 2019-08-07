/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.internal.MutabilityOwnership


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1

/**
 * Gets trie index segment of the specified [index] at the level specified by [shift].
 *
 * `shift` equal to zero corresponds to the root level.
 * For each lower level `shift` increments by [LOG_MAX_BRANCHING_FACTOR].
 */
internal fun indexSegment(index: Int, shift: Int): Int =
        (index shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE


private fun <E> Array<Any?>.addElementAtIndex(index: Int, element: E): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size + 1)
    this.copyInto(newBuffer, endIndex = index)
    this.copyInto(newBuffer, index + 1, index, this.size)
    newBuffer[index] = element
    return newBuffer
}

private fun Array<Any?>.removeCellAtIndex(cellIndex: Int): Array<Any?> {
    val newBuffer = arrayOfNulls<Any?>(this.size - 1)
    this.copyInto(newBuffer, endIndex = cellIndex)
    this.copyInto(newBuffer, cellIndex, cellIndex + 1, this.size)
    return newBuffer
}

internal interface TrieNode<E> {
    val buffer: Array<Any?>

    fun isCollision(): Boolean

    fun contains(elementHash: Int, element: E, shift: Int): Boolean

    fun add(elementHash: Int, element: E, shift: Int): TrieNode<E>

    fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>

    fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>?

    fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>?
}


private class CollisionTrieNode<E>(
        val collisionHash: Int,
        override var buffer: Array<Any?>,
        val ownedBy: MutabilityOwnership?
): TrieNode<E> {

    constructor(buffer: Array<Any?>, collisionHash: Int) : this(collisionHash, buffer, null)

    private fun lastBits(hash: Int, shift: Int): Int {
        return hash and ((1 shl shift) - 1)
    }

    private fun makeNode(elementHash: Int, element: E, shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        assert(elementHash != collisionHash)

        val collisionBits = indexSegment(collisionHash, shift)
        val elementBits = indexSegment(elementHash, shift)

        if (collisionBits != elementBits) {
            val nodeBuffer = if (collisionBits < elementBits) {
                arrayOf<Any?>(this, element)
            } else {
                arrayOf<Any?>(element, this)
            }
            return CompactTrieNode((1 shl collisionBits) or (1 shl elementBits), nodeBuffer, owner)
        }

        val node = makeNode(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return CompactTrieNode<E>(1 shl collisionBits, arrayOf(node), owner)
    }

    override fun isCollision(): Boolean {
        return true
    }

    override fun contains(elementHash: Int, element: E, shift: Int): Boolean {
//        assert(lastBits(elementHash, shift) == lastBits(collisionHash, shift))

        if (collisionHash != elementHash) {
            return false
        }
        return buffer.contains(element)
    }

    override fun add(elementHash: Int, element: E, shift: Int): TrieNode<E> {
//        assert(lastBits(elementHash, shift) == lastBits(collisionHash, shift))

        if (collisionHash != elementHash) {
            return makeNode(elementHash, element, shift, null)
        }
        if (buffer.contains(element)) {
            return this
        }
        return CollisionTrieNode(buffer + element, collisionHash)
    }

    override fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
//        assert(lastBits(elementHash, shift) == lastBits(collisionHash, shift))

        if (collisionHash != elementHash) {
            mutator.size++
            return makeNode(elementHash, element, shift, mutator.ownership)
        }
        if (buffer.contains(element)) {
            return this
        }

        mutator.size++

        if (ownedBy === mutator.ownership) {
            buffer += element
            return this
        }
        return CollisionTrieNode(collisionHash, buffer + element, mutator.ownership)
    }

    override fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>? {
//        assert(lastBits(elementHash, shift) == lastBits(collisionHash, shift))

        if (collisionHash != elementHash) {
            return this
        }

        for (index in 0 until buffer.size) {
            if (buffer[index] == element) {
                if (buffer.size == 1) {
                    return null
                }
                return CollisionTrieNode(buffer.removeCellAtIndex(index), collisionHash)
            }
        }
        return this
    }

    override fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
//        assert(lastBits(elementHash, shift) == lastBits(collisionHash, shift))

        if (collisionHash != elementHash) {
            return this
        }

        for (index in 0 until buffer.size) {
            if (buffer[index] == element) {
                mutator.size--

                if (buffer.size == 1) {
                    return null
                }
                if (ownedBy === mutator.ownership) {
                    buffer = buffer.removeCellAtIndex(index)
                    return this
                }
                return CollisionTrieNode(collisionHash, buffer.removeCellAtIndex(index), mutator.ownership)
            }
        }
        return this
    }
}

internal class CompactTrieNode<E>(
        var bitmap: Int,
        override var buffer: Array<Any?>,
        var ownedBy: MutabilityOwnership?
): TrieNode<E> {

    constructor(bitmap: Int, buffer: Array<Any?>) : this(bitmap, buffer, null)

    // here and later:
    // positionMask — an int in form 2^n, i.e. having the single bit set, whose ordinal is a logical position in buffer

    private fun hasNoCellAt(positionMask: Int): Boolean {
        return bitmap and positionMask == 0
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    private fun indexOfCellAt(positionMask: Int): Int {
        return (bitmap and (positionMask - 1)).countOneBits()
    }

    private fun elementAtIndex(index: Int): E {
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as E
    }

    private fun nodeAtIndex(index: Int): TrieNode<E> {
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as TrieNode<E>
    }

    private fun addElementAt(positionMask: Int, element: E): TrieNode<E> {
//        assert(hasNoCellAt(positionMask))

        val index = indexOfCellAt(positionMask)
        val newBuffer = buffer.addElementAtIndex(index, element)
        return CompactTrieNode(bitmap or positionMask, newBuffer)
    }

    private fun mutableAddElementAt(positionMask: Int, element: E, owner: MutabilityOwnership): TrieNode<E> {
//        assert(hasNoCellAt(positionMask))

        val index = indexOfCellAt(positionMask)
        if (ownedBy === owner) {
            buffer = buffer.addElementAtIndex(index, element)
            bitmap = bitmap or positionMask
            return this
        }
        val newBuffer = buffer.addElementAtIndex(index, element)
        return CompactTrieNode(bitmap or positionMask, newBuffer, owner)
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return CompactTrieNode(bitmap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>, owner: MutabilityOwnership): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        if (ownedBy === owner) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return CompactTrieNode(bitmap, newBuffer, owner)
    }

    private fun makeNodeAtIndex(elementIndex: Int, newElementHash: Int, newElement: E,
                                shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        val storedElement = elementAtIndex(elementIndex)
        val storedElementHash = storedElement.hashCode()

        if (storedElementHash == newElementHash) {
            return CollisionTrieNode(storedElementHash, arrayOf<Any?>(storedElement, newElement), owner)
        }

        return makeNode(storedElementHash, storedElement,
                newElementHash, newElement, shift + LOG_MAX_BRANCHING_FACTOR, owner)
    }

    private fun moveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                  shift: Int): TrieNode<E> {
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, null)
        return CompactTrieNode(bitmap, newBuffer)
    }

    private fun mutableMoveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                         shift: Int, owner: MutabilityOwnership): TrieNode<E> {
        if (ownedBy === owner) {
            buffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, owner)
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, owner)
        return CompactTrieNode(bitmap, newBuffer, owner)
    }

    private fun makeNode(elementHash1: Int, element1: E, elementHash2: Int, element2: E,
                         shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        assert(elementHash1 != elementHash2)

        val setBit1 = indexSegment(elementHash1, shift)
        val setBit2 = indexSegment(elementHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf<Any?>(element1, element2)
            } else {
                arrayOf<Any?>(element2, element1)
            }
            return CompactTrieNode((1 shl setBit1) or (1 shl setBit2), nodeBuffer, owner)
        }
        // hash segments at the given shift are equal: move these elements into the subtrie
        val node = makeNode(elementHash1, element1, elementHash2, element2, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return CompactTrieNode<E>(1 shl setBit1, arrayOf(node), owner)
    }


    private fun removeCellAtIndex(cellIndex: Int, positionMask: Int): TrieNode<E>? {
//        assert(!hasNoCellAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return CompactTrieNode(bitmap xor positionMask, newBuffer)
    }

    private fun mutableRemoveCellAtIndex(cellIndex: Int, positionMask: Int, owner: MutabilityOwnership): TrieNode<E>? {
//        assert(!hasNoCellAt(positionMask))
        if (buffer.size == 1) return null

        if (ownedBy === owner) {
            buffer = buffer.removeCellAtIndex(cellIndex)
            bitmap = bitmap xor positionMask
            return this
        }
        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return CompactTrieNode(bitmap xor positionMask, newBuffer, owner)
    }

    override fun isCollision(): Boolean {
        return false
    }

    override fun contains(elementHash: Int, element: E, shift: Int): Boolean {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return false
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            return targetNode.contains(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
        }
        // element is directly in buffer
        return element == buffer[cellIndex]
    }

    override fun add(elementHash: Int, element: E, shift: Int): TrieNode<E> {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return addElementAt(cellPositionMask, element)
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = targetNode.add(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            if (targetNode === newNode) return this
            return updateNodeAtIndex(cellIndex, newNode)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) return this
        return moveElementToNode(cellIndex, elementHash, element, shift)
    }

    override fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val cellPosition = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPosition)) { // element is absent
            mutator.size++
            return mutableAddElementAt(cellPosition, element, mutator.ownership)
        }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = targetNode.mutableAdd(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            if (targetNode === newNode) return this
            return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.ownership)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) return this
        mutator.size++
        return mutableMoveElementToNode(cellIndex, elementHash, element, shift, mutator.ownership)
    }

    override fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>? {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = targetNode.remove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            return when {
                targetNode === newNode -> this
                newNode == null -> removeCellAtIndex(cellIndex, cellPositionMask)
                else -> updateNodeAtIndex(cellIndex, newNode)
            }
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            return removeCellAtIndex(cellIndex, cellPositionMask)
        }
        return this
    }

    override fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = targetNode.mutableRemove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveCellAtIndex(cellIndex, cellPositionMask, mutator.ownership)
                else -> mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.ownership)
            }
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            mutator.size--
            return mutableRemoveCellAtIndex(cellIndex, cellPositionMask, mutator.ownership)   // check is empty
        }
        return this
    }

    internal companion object {
        internal val EMPTY = CompactTrieNode<Nothing>(0, emptyArray())
    }
}