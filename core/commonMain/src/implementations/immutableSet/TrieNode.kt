/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.internal.DeltaCounter
import kotlinx.collections.immutable.internal.MutabilityOwnership
import kotlinx.collections.immutable.internal.assert
import kotlinx.collections.immutable.internal.forEachOneBit


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

    internal fun indexOfCellAt(positionMask: Int): Int {
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

    private fun collisionAddAll(otherNode: TrieNode<E>, intersectionSizeRef: DeltaCounter): TrieNode<E> {
        val resultBuffer = buffer.copyOf(buffer.size + otherNode.buffer.size)
        var filledSize = buffer.size
        for (e in otherNode.buffer) {
            assert(e !is TrieNode<*>)
            if (!buffer.contains(e)) resultBuffer[filledSize++] = e
        }
        intersectionSizeRef += resultBuffer.size - filledSize
        if (filledSize == buffer.size) return this
        return TrieNode(0, resultBuffer.copyOf(filledSize))
    }

    private fun mutableCollisionAddAll(otherNode: TrieNode<E>, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        var result = this
        for (e in otherNode.buffer) {
            assert(e !is TrieNode<*>)
            mutator.size--
            @Suppress("UNCHECKED_CAST")
            val newNode = result.mutableCollisionAdd(e as E, mutator)
            result = newNode
        }
        return result
    }

    private fun calculateSize(): Int {
        if (bitmap == 0) return buffer.size
        var result = 0
        for (e in buffer) {
            result += when(e) {
                is TrieNode<*> -> e.calculateSize()
                else -> 1
            }
        }
        return result
    }

    private fun elementsEquals(otherNode: TrieNode<E>): Boolean {
        if (this === otherNode) return true
        if (this.buffer === otherNode.buffer) return true
        if (bitmap != otherNode.bitmap) return false
        for (i in 0 until buffer.size) {
            if(buffer[i] !== otherNode.buffer[i]) return false
        }
        return true
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

    fun addAll(otherNode: TrieNode<E>, shift: Int, intersectionSizeRef: DeltaCounter): TrieNode<E> {
        if (this === otherNode) {
            intersectionSizeRef += calculateSize()
            return this
        }
        if (shift > MAX_SHIFT) {
            return collisionAddAll(otherNode, intersectionSizeRef)
        }
        // union mask contains all the bits from input masks
        val newBitMap = bitmap or otherNode.bitmap
        // first allocate the node and then fill it in
        // we are doing a union, so all the array elements are guaranteed to exist
        val newNode = TrieNode<E>(newBitMap, arrayOfNulls<Any?>(newBitMap.countOneBits()))
        // for each bit set in the resulting mask,
        // either left, right or both masks contain the same bit
        // Note: we shouldn't overrun MAX_SHIFT because both sides are correct TrieNodes, right?
        var newNodeIndex = 0
        newBitMap.forEachOneBit { positionMask ->
            val thisIndex = indexOfCellAt(positionMask)
            val otherNodeIndex = otherNode.indexOfCellAt(positionMask)
            newNode.buffer[newNodeIndex++] = when {
                // no element on left -> pick right
                hasNoCellAt(positionMask) -> otherNode.buffer[otherNodeIndex]
                // no element on right -> pick left
                otherNode.hasNoCellAt(positionMask) -> buffer[thisIndex]
                // both nodes contain something at the masked bit
                else -> {
                    val thisCell = buffer[thisIndex]
                    val otherNodeCell = otherNode.buffer[otherNodeIndex]
                    val thisIsNode = thisCell is TrieNode<*>
                    val otherIsNode = otherNodeCell is TrieNode<*>
                    when {
                        // both are nodes -> merge them recursively
                        thisIsNode && otherIsNode -> @Suppress("UNCHECKED_CAST") {
                            thisCell as TrieNode<E>
                            otherNodeCell as TrieNode<E>
                            thisCell.addAll(
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    intersectionSizeRef
                            )
                        }
                        // one of them is a node -> add the other one to it
                        thisIsNode -> @Suppress("UNCHECKED_CAST") {
                            thisCell as TrieNode<E>
                            otherNodeCell as E
                            thisCell.add(
                                    otherNodeCell.hashCode(),
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR
                            ).also {
                                if (it === thisCell) intersectionSizeRef += 1
                            }
                        }
                        // same as last case, but reversed
                        otherIsNode -> @Suppress("UNCHECKED_CAST") {
                            otherNodeCell as TrieNode<E>
                            thisCell as E
                            otherNodeCell.add(
                                    thisCell.hashCode(),
                                    thisCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR
                            ).also {
                                if (it === otherNodeCell) intersectionSizeRef += 1
                            }
                        }
                        // both are just E => compare them
                        thisCell == otherNodeCell -> thisCell.also { intersectionSizeRef += 1 }
                        // both are just E, but different => make a collision-ish node
                        else -> @Suppress("UNCHECKED_CAST") {
                            thisCell as E
                            otherNodeCell as E
                            makeNode(
                                    thisCell.hashCode(),
                                    thisCell,
                                    otherNodeCell.hashCode(),
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    null
                            )
                        }
                    }
                }
            }
        }
        return when {
            this.elementsEquals(newNode) -> this
            otherNode.elementsEquals(newNode) -> otherNode
            else -> newNode
        }
    }

    fun mutableAddAll(otherNode: TrieNode<E>, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        if (this === otherNode) {
            mutator.size -= this.calculateSize()
            return this
        }
        if (shift > MAX_SHIFT) {
            return mutableCollisionAddAll(otherNode, mutator)
        }
        // union mask contains all the bits from input masks
        val newBitMap = bitmap or otherNode.bitmap
        // first allocate the node and then fill it in
        // we are doing a union, so all the array elements are guaranteed to exist
        val mutableNode = when {
            newBitMap == bitmap && ownedBy == mutator.ownership -> this
            newBitMap == otherNode.bitmap && otherNode.ownedBy == mutator.ownership -> otherNode
            else -> TrieNode<E>(newBitMap, arrayOfNulls<Any?>(newBitMap.countOneBits()), mutator.ownership)
        }
        // for each bit set in the resulting mask,
        // either left, right or both masks contain the same bit
        // Note: we shouldn't overrun MAX_SHIFT because both sides are correct TrieNodes, right?
        newBitMap.forEachOneBit { positionMask ->
            val newNodeIndex = mutableNode.indexOfCellAt(positionMask)
            val thisIndex = indexOfCellAt(positionMask)
            val otherNodeIndex = otherNode.indexOfCellAt(positionMask)
            mutableNode.buffer[newNodeIndex] = when {
                // no element on left -> pick right
                hasNoCellAt(positionMask) -> otherNode.buffer[otherNodeIndex]
                // no element on right -> pick left
                otherNode.hasNoCellAt(positionMask) -> buffer[thisIndex]
                // both nodes contain something at the masked bit
                else -> {
                    val thisCell = buffer[thisIndex]
                    val otherNodeCell = otherNode.buffer[otherNodeIndex]
                    val thisIsNode = thisCell is TrieNode<*>
                    val otherIsNode = otherNodeCell is TrieNode<*>
                    when {
                        // both are nodes -> merge them recursively
                        thisIsNode && otherIsNode -> @Suppress("UNCHECKED_CAST") {
                            thisCell as TrieNode<E>
                            otherNodeCell as TrieNode<E>
                            thisCell.mutableAddAll(
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    mutator
                            )
                        }
                        // one of them is a node -> add the other one to it
                        thisIsNode -> @Suppress("UNCHECKED_CAST") {
                            thisCell as TrieNode<E>
                            otherNodeCell as E
                            mutator.size--
                            thisCell.mutableAdd(
                                    otherNodeCell.hashCode(),
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    mutator
                            )
                        }
                        // same as last case, but reversed
                        otherIsNode -> @Suppress("UNCHECKED_CAST") {
                            otherNodeCell as TrieNode<E>
                            thisCell as E
                            mutator.size--
                            otherNodeCell.mutableAdd(
                                    thisCell.hashCode(),
                                    thisCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    mutator
                            )
                        }
                        // both are just E => compare them
                        thisCell == otherNodeCell -> thisCell
                        // both are just E, but different => make a collision-ish node
                        else -> @Suppress("UNCHECKED_CAST") {
                            thisCell as E
                            otherNodeCell as E
                            makeNode(
                                    thisCell.hashCode(),
                                    thisCell,
                                    otherNodeCell.hashCode(),
                                    otherNodeCell,
                                    shift + LOG_MAX_BRANCHING_FACTOR,
                                    mutator.ownership
                            )
                        }
                    }
                }
            }
        }
        return when {
            this.elementsEquals(mutableNode) -> this
            otherNode.elementsEquals(mutableNode) -> otherNode
            else -> mutableNode
        }
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