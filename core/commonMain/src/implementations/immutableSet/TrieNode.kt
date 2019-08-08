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

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(bitmap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>, owner: MutabilityOwnership): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        if (ownedBy === owner) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(bitmap, newBuffer, owner)
    }

    private fun moveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                  shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
        val storedElement = elementAtIndex(elementIndex)
        val storedElementHash = storedElement.hashCode()

        val newNode: TrieNode<E>

        if (storedElementHash == newElementHash) {
            newNode = makeCollisionNode(storedElement, newElement, owner)
            if (buffer.size == 1) {
                return newNode
            }
        } else {
            newNode = makeNode(storedElementHash, storedElement,
                    newElementHash, newElement, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        }

        if (ownedBy != null && ownedBy === owner) {
            buffer[elementIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = newNode
        return TrieNode(bitmap, newBuffer, owner)
    }

    private fun makeCollisionNode(element1: E, element2: E, owner: MutabilityOwnership?): TrieNode<E> {
        return makeCollisionNode(arrayOf<Any?>(element1, element2), owner)
    }

    private fun makeCollisionNode(buffer: Array<Any?>, owner: MutabilityOwnership?): TrieNode<E> {
        return TrieNode(0, buffer, owner)
    }

    private fun makeNode(elementHash1: Int, element1: E, elementHash2: Int, element2: E,
                         shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
//        assert(elementHash1 != elementHash2)

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

    private fun makeNode(collisionHash: Int, elementHash: Int, element: E, shift: Int, owner: MutabilityOwnership?): TrieNode<E> {
//        assert(elementHash != collisionHash)

        val collisionBits = indexSegment(collisionHash, shift)
        val elementBits = indexSegment(elementHash, shift)

        if (collisionBits != elementBits) {
            val nodeBuffer = if (collisionBits < elementBits) {
                arrayOf<Any?>(this, element)
            } else {
                arrayOf<Any?>(element, this)
            }
            return TrieNode((1 shl collisionBits) or (1 shl elementBits), nodeBuffer, owner)
        }

        val node = makeNode(collisionHash, elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, owner)
        return TrieNode<E>(1 shl collisionBits, arrayOf(node), owner)
    }


    private fun removeCellAtIndex(cellIndex: Int, positionMask: Int): TrieNode<E>? {
//        assert(!hasNoCellAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return TrieNode(bitmap xor positionMask, newBuffer)
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
        return TrieNode(bitmap xor positionMask, newBuffer, owner)
    }

    private fun collisionContains(elementHash: Int, element: E): Boolean {
        if (elementHash != buffer[0].hashCode()) {
            return false
        }
        return buffer.contains(element)
    }

    private fun collisionAdd(elementHash: Int, element: E, shift: Int): TrieNode<E> {
        val collisionHash = buffer[0].hashCode()
        if (elementHash != collisionHash) {
            return makeNode(collisionHash, elementHash, element, shift, null)
        }
        if (buffer.contains(element)) {
            return this
        }
        return makeCollisionNode(buffer + element, null)
    }

    private fun collisionMutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val collisionHash = buffer[0].hashCode()
        if (collisionHash != elementHash) {
            mutator.size++
            return makeNode(collisionHash, elementHash, element, shift, mutator.ownership)
        }
        if (buffer.contains(element)) {
            return this
        }

        mutator.size++

        if (ownedBy === mutator.ownership) {
            buffer += element
            return this
        }
        return makeCollisionNode(buffer + element, mutator.ownership)
    }

    private fun collisionRemove(elementHash: Int, element: E): TrieNode<E>? {
        if (buffer[0].hashCode() != elementHash) {
            return this
        }
        val index = buffer.indexOf(element)
        if (index == -1) return this
        if (buffer.size == 1) return null
        return makeCollisionNode(buffer.removeCellAtIndex(index), null)
    }

    private fun collisionMutableRemove(elementHash: Int, element: E, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
        if (buffer[0].hashCode() != elementHash) {
            return this
        }

        val index = buffer.indexOf(element)
        if (index == -1) return this

        mutator.size--

        if (buffer.size == 1) return null
        if (ownedBy === mutator.ownership) {
            buffer = buffer.removeCellAtIndex(index)
            return this
        }
        return makeCollisionNode(buffer.removeCellAtIndex(index), mutator.ownership)
    }

    fun isCollision(): Boolean {
        return bitmap == 0 && buffer.isNotEmpty()
    }

    fun contains(elementHash: Int, element: E, shift: Int): Boolean {
        if (isCollision()) {
            return collisionContains(elementHash, element)
        }

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

    fun add(elementHash: Int, element: E, shift: Int): TrieNode<E> {
        if (isCollision()) {
            return collisionAdd(elementHash, element, shift)
        }

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
        return moveElementToNode(cellIndex, elementHash, element, shift, null)
    }

    fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        if (isCollision()) {
            return collisionMutableAdd(elementHash, element, shift, mutator)
        }

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
        return moveElementToNode(cellIndex, elementHash, element, shift, mutator.ownership)
    }

    fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>? {
        if (isCollision()) {
            return collisionRemove(elementHash, element)
        }

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

    fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
        if (isCollision()) {
            return collisionMutableRemove(elementHash, element, mutator)
        }

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
        internal val EMPTY = TrieNode<Nothing>(0, emptyArray())
    }
}