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

package kotlinx.collections.immutable.implementations.immutableSet


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
        var marker: Marker?
) {

    constructor(bitmap: Int, buffer: Array<Any?>) : this(bitmap, buffer, null)

    // here and later:
    // positionMask — an int in form 2^n, i.e. having the single bit set, whose ordinal is a logical position in buffer

    private fun hasNoCellAt(positionMask: Int): Boolean {
        return bitmap and positionMask == 0
    }

    private fun indexOfCellAt(positionMask: Int): Int {
        return Integer.bitCount(bitmap and (positionMask - 1))
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

    private fun mutableAddElementAt(positionMask: Int, element: E, mutatorMarker: Marker): TrieNode<E> {
//        assert(hasNoCellAt(positionMask))

        val index = indexOfCellAt(positionMask)
        if (marker === mutatorMarker) {
            buffer = buffer.addElementAtIndex(index, element)
            bitmap = bitmap or positionMask
            return this
        }
        val newBuffer = buffer.addElementAtIndex(index, element)
        return TrieNode(bitmap or positionMask, newBuffer, mutatorMarker)
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(bitmap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: TrieNode<E>, mutatorMarker: Marker): TrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        if (marker === mutatorMarker) {
            buffer[nodeIndex] = newNode
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return TrieNode(bitmap, newBuffer, mutatorMarker)
    }

    private fun makeNodeAtIndex(elementIndex: Int, newElementHash: Int, newElement: E,
                                shift: Int, mutatorMarker: Marker?): TrieNode<E> {
        val storedElement = elementAtIndex(elementIndex)
        return makeNode(storedElement.hashCode(), storedElement,
                newElementHash, newElement, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
    }

    private fun moveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                  shift: Int): TrieNode<E> {
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, null)
        return TrieNode(bitmap, newBuffer)
    }

    private fun mutableMoveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                         shift: Int, mutatorMarker: Marker): TrieNode<E> {
        if (marker === mutatorMarker) {
            buffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, mutatorMarker)
            return this
        }
        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, mutatorMarker)
        return TrieNode(bitmap, newBuffer, mutatorMarker)
    }

    private fun makeNode(elementHash1: Int, element1: E, elementHash2: Int, element2: E,
                         shift: Int, mutatorMarker: Marker?): TrieNode<E> {
        if (shift > MAX_SHIFT) {
//            assert(element1 != element2)
            // when two element hashes are entirely equal: the last level subtrie node stores them just as unordered list
            return TrieNode<E>(0, arrayOf(element1, element2), mutatorMarker)
        }

        val setBit1 = indexSegment(elementHash1, shift)
        val setBit2 = indexSegment(elementHash2, shift)

        if (setBit1 != setBit2) {
            val nodeBuffer = if (setBit1 < setBit2) {
                arrayOf<Any?>(element1, element2)
            } else {
                arrayOf<Any?>(element2, element1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), nodeBuffer, mutatorMarker)
        }
        // hash segments at the given shift are equal: move these elements into the subtrie
        val node = makeNode(elementHash1, element1, elementHash2, element2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
        return TrieNode<E>(1 shl setBit1, arrayOf(node), mutatorMarker)
    }


    private fun removeCellAtIndex(cellIndex: Int, positionMask: Int): TrieNode<E>? {
//        assert(!hasNoCellAt(positionMask))
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return TrieNode(bitmap xor positionMask, newBuffer)
    }

    private fun mutableRemoveCellAtIndex(cellIndex: Int, positionMask: Int, mutatorMarker: Marker): TrieNode<E>? {
//        assert(!hasNoCellAt(positionMask))
        if (buffer.size == 1) return null

        if (marker === mutatorMarker) {
            buffer = buffer.removeCellAtIndex(cellIndex)
            bitmap = bitmap xor positionMask
            return this
        }
        val newBuffer = buffer.removeCellAtIndex(cellIndex)
        return TrieNode(bitmap xor positionMask, newBuffer, mutatorMarker)
    }

    private fun collisionRemoveElementAtIndex(i: Int): TrieNode<E>? {
        if (buffer.size == 1) return null

        val newBuffer = buffer.removeCellAtIndex(i)
        return TrieNode(0, newBuffer)
    }

    private fun mutableCollisionRemoveElementAtIndex(i: Int, mutatorMarker: Marker): TrieNode<E>? {
        if (buffer.size == 1) return null

        if (marker === mutatorMarker) {
            buffer = buffer.removeCellAtIndex(i)
            return this
        }
        val newBuffer = buffer.removeCellAtIndex(i)
        return TrieNode(0, newBuffer, mutatorMarker)
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
        if (marker === mutator.marker) {
            buffer = buffer.addElementAtIndex(0, element)
            return this
        }
        val newBuffer = buffer.addElementAtIndex(0, element)
        return TrieNode(0, newBuffer, mutator.marker)
    }

    private fun collisionRemove(element: E): TrieNode<E>? {
        val index = buffer.indexOf(element)
        if (index != -1) {
            return collisionRemoveElementAtIndex(index)
        }
        return this
    }

    private fun mutableCollisionRemove(element: E, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
        val index = buffer.indexOf(element)
        if (index != -1) {
            mutator.size--
            return mutableCollisionRemoveElementAtIndex(index, mutator.marker)
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
            return mutableAddElementAt(cellPosition, element, mutator.marker)
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
            return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.marker)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) return this
        mutator.size++
        return mutableMoveElementToNode(cellIndex, elementHash, element, shift, mutator.marker)
    }

    fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>? {
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
            return when {
                targetNode === newNode -> this
                newNode == null -> mutableRemoveCellAtIndex(cellIndex, cellPositionMask, mutator.marker)
                else -> mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.marker)
            }
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            mutator.size--
            return mutableRemoveCellAtIndex(cellIndex, cellPositionMask, mutator.marker)   // check is empty
        }
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing>(0, emptyArray())
    }
}