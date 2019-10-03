/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.internal.MutabilityOwnership


internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val MAX_SHIFT = 30

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

internal class TrieNode<E>(
        var bitmap: Int,
        var buffer: Array<Any?>,
        var ownedBy: MutabilityOwnership?
) {

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
        return TrieNode(bitmap or positionMask, newBuffer)
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
        return TrieNode(bitmap or positionMask, newBuffer, owner)
    }

    /** The given [newNode] must not be a part of any persistent set instance. */
    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)
        val cell: Any?

        val newNodeBuffer = newNode.buffer
        if (newNodeBuffer.size == 1 && newNodeBuffer[0] !is TrieNode<*>) {
            if (buffer.size == 1) {
                newNode.bitmap = bitmap
                return newNode
            }
            cell = newNodeBuffer[0]
        } else {
            cell = newNode
        }

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = cell
        return TrieNode(bitmap, newBuffer)
    }

    /** The given [newNode] must not be a part of any persistent set instance. */
    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>, owner: MutabilityOwnership): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        val cell: Any?

        val newNodeBuffer = newNode.buffer
        if (newNodeBuffer.size == 1 && newNodeBuffer[0] !is TrieNode<*>) {
            if (buffer.size == 1) {
                newNode.bitmap = bitmap
                return newNode
            }
            cell = newNodeBuffer[0]
        } else {
            cell = newNode
        }

        if (ownedBy === owner) {
            buffer[nodeIndex] = cell
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = cell
        return TrieNode(bitmap, newBuffer, owner)
    }

    private fun makeNodeAtIndex(elementIndex: Int, newElementHash: Int, newElement: E,
                                shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        val storedElement = elementAtIndex(elementIndex)
        return makeNode(storedElement.hashCode(), storedElement,
                newElementHash, newElement, shift + LOG_MAX_BRANCHING_FACTOR, owner)
    }

    private fun moveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                  shift: Int): TrieNode<E> {
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, null)
        return TrieNode(bitmap, newBuffer)
    }

    private fun mutableMoveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                         shift: Int, owner: MutabilityOwnership): TrieNode<E> {
        if (ownedBy === owner) {
            buffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, owner)
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, owner)
        return TrieNode(bitmap, newBuffer, owner)
    }

    private fun makeNode(elementHash1: Int, element1: E, elementHash2: Int, element2: E,
                         shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        if (shift > MAX_SHIFT) {
//            assert(element1 != element2)
            // when two element hashes are entirely equal: the last level subtrie node stores them just as unordered list
            return TrieNode<E>(0, arrayOf(element1, element2), owner)
        }

        val setBit1 = indexSegment(elementHash1, shift)
        val setBit2 = indexSegment(elementHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf<Any?>(element1, element2)
            } else {
                arrayOf<Any?>(element2, element1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), nodeBuffer, owner)
        }
        // hash segments at the given shift are equal: move these elements into the subtrie
        val node = makeNode(elementHash1, element1, elementHash2, element2, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return TrieNode<E>(1 shl setBit1, arrayOf(node), owner)
    }


    private fun removeCellAtIndex(cellIndex: Int, positionMask: Int): TrieNode<E> {
//        assert(!hasNoCellAt(positionMask))
//        assert(buffer.size > 1) can be false only for the root node

        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return TrieNode(bitmap xor positionMask, newBuffer)
    }

    private fun mutableRemoveCellAtIndex(cellIndex: Int, positionMask: Int, owner: MutabilityOwnership): TrieNode<E> {
//        assert(!hasNoCellAt(positionMask))
//        assert(buffer.size > 1)

        if (ownedBy === owner) {
            buffer = buffer.removeCellAtIndex(cellIndex)
            bitmap = bitmap xor positionMask
            return this
        }
        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return TrieNode(bitmap xor positionMask, newBuffer, owner)
    }

    private fun collisionRemoveElementAtIndex(i: Int): TrieNode<E> {
        val newBuffer = buffer.removeCellAtIndex(i)
        return TrieNode(0, newBuffer)
    }

    private fun mutableCollisionRemoveElementAtIndex(i: Int, owner: MutabilityOwnership): TrieNode<E> {
        if (ownedBy === owner) {
            buffer = buffer.removeCellAtIndex(i)
            return this
        }
        val newBuffer = buffer.removeCellAtIndex(i)
        return TrieNode(0, newBuffer, owner)
    }

    private fun collisionContainsElement(element: E): Boolean {
        return buffer.contains(element)
    }

    private fun collisionAdd(element: E): TrieNode<E> {
        if (collisionContainsElement(element)) return this
        val newBuffer = buffer.addElementAtIndex(0, element)
        return TrieNode(0, newBuffer)
    }

    private fun mutableCollisionAdd(element: E, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        if (collisionContainsElement(element)) return this
        mutator.size++
        if (ownedBy === mutator.ownership) {
            buffer = buffer.addElementAtIndex(0, element)
            return this
        }
        val newBuffer = buffer.addElementAtIndex(0, element)
        return TrieNode(0, newBuffer, mutator.ownership)
    }

    private fun collisionRemove(element: E): TrieNode<E> {
        val index = buffer.indexOf(element)
        if (index != -1) {
            return collisionRemoveElementAtIndex(index)
        }
        return this
    }

    private fun mutableCollisionRemove(element: E, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val index = buffer.indexOf(element)
        if (index != -1) {
            mutator.size--
            return mutableCollisionRemoveElementAtIndex(index, mutator.ownership)
        }
        return this
    }

    fun contains(elementHash: Int, element: E, shift: Int): Boolean {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return false
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            if (shift == MAX_SHIFT) {
                return targetNode.collisionContainsElement(element)
            }
            return targetNode.contains(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
        }
        // element is directly in buffer
        return element == buffer[cellIndex]
    }

    fun add(elementHash: Int, element: E, shift: Int): TrieNode<E> {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return addElementAt(cellPositionMask, element)
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionAdd(element)
            } else {
                targetNode.add(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) return this
            return updateNodeAtIndex(cellIndex, newNode)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) return this
        return moveElementToNode(cellIndex, elementHash, element, shift)
    }

    fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val cellPosition = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPosition)) { // element is absent
            mutator.size++
            return mutableAddElementAt(cellPosition, element, mutator.ownership)
        }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionAdd(element, mutator)
            } else {
                targetNode.mutableAdd(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode === newNode) return this
            return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.ownership)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) return this
        mutator.size++
        return mutableMoveElementToNode(cellIndex, elementHash, element, shift, mutator.ownership)
    }

    fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E> {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(element)
            } else {
                targetNode.remove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) return this
            return updateNodeAtIndex(cellIndex, newNode)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            return removeCellAtIndex(cellIndex, cellPositionMask)
        }
        return this
    }

    fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val cellPositionMask = 1 shl indexSegment(elementHash, shift)

        if (hasNoCellAt(cellPositionMask)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPositionMask)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(element, mutator)
            } else {
                targetNode.mutableRemove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (ownedBy === mutator.ownership || targetNode !== newNode) {
                return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.ownership)
            }
            return this
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            mutator.size--
            return mutableRemoveCellAtIndex(cellIndex, cellPositionMask, mutator.ownership)   // check is empty
        }
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing>(0, emptyArray())
    }
}