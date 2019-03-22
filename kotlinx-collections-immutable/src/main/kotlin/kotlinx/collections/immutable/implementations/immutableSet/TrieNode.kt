/*
 * Copyright 2016-2018 JetBrains s.r.o.
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
internal const val ENTRY_SIZE = 2
internal const val MAX_SHIFT = 30


internal class TrieNode<E>(var bitmap: Int,
                           var buffer: Array<Any?>,
                           var marker: Marker?) {

    constructor(bitmap: Int, buffer: Array<Any?>) : this(bitmap, buffer, null)

    private fun isNullCellAt(position: Int): Boolean {
        return bitmap and position == 0
    }

    private fun indexOfCellAt(position: Int): Int {
        return Integer.bitCount(bitmap and (position - 1))
    }

    private fun elementAtIndex(index: Int): E {
        return buffer[index] as E
    }

    private fun nodeAtIndex(index: Int): TrieNode<E> {
        return buffer[index] as TrieNode<E>
    }

    private fun bufferAddElementAtIndex(index: Int, element: E): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 1)
        System.arraycopy(buffer, 0, newBuffer, 0, index)
        System.arraycopy(buffer, index, newBuffer, index + 1, buffer.size - index)
        newBuffer[index] = element
        return newBuffer
    }

    private fun addElementAt(position: Int, element: E): TrieNode<E> {
//        assert(isNullCellAt(position))

        val index = indexOfCellAt(position)
        val newBuffer = bufferAddElementAtIndex(index, element)
        return TrieNode(bitmap or position, newBuffer)
    }

    private fun mutableAddElementAt(position: Int, element: E, mutatorMarker: Marker): TrieNode<E> {
//        assert(isNullCellAt(position))

        val index = indexOfCellAt(position)
        if (marker === mutatorMarker) {
            buffer = bufferAddElementAtIndex(index, element)
            bitmap = bitmap or position
            return this
        }
        val newBuffer = bufferAddElementAtIndex(index, element)
        return TrieNode(bitmap or position, newBuffer, mutatorMarker)
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
//        assert(!isNullCellAt(position))

        val newBuffer = buffer.copyOf()
        newBuffer[elementIndex] = makeNodeAtIndex(elementIndex, newElementHash, newElement, shift, null)
        return TrieNode(bitmap, newBuffer)
    }

    private fun mutableMoveElementToNode(elementIndex: Int, newElementHash: Int, newElement: E,
                                         shift: Int, mutatorMarker: Marker): TrieNode<E> {
//        assert(!isNullCellAt(position))

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
            return TrieNode<E>(0, arrayOf(element1, element2), mutatorMarker)
        }

        val setBit1 = (elementHash1 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
        val setBit2 = (elementHash2 shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE

        if (setBit1 != setBit2) {
            val nodeBuffer =  if (setBit1 < setBit2) {
                arrayOf<Any?>(element1, element2)
            } else {
                arrayOf<Any?>(element2, element1)
            }
            return TrieNode((1 shl setBit1) or (1 shl setBit2), nodeBuffer, mutatorMarker)
        }
        val node = makeNode(elementHash1, element1, elementHash2, element2, shift + LOG_MAX_BRANCHING_FACTOR, mutatorMarker)
        return TrieNode<E>(1 shl setBit1, arrayOf(node), mutatorMarker)
    }

    private fun bufferRemoveCellAtIndex(cellIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, cellIndex)
        System.arraycopy(buffer, cellIndex + 1, newBuffer, cellIndex, buffer.size - cellIndex - 1)
        return newBuffer
    }

    private fun removeCellAtIndex(cellIndex: Int, position: Int): TrieNode<E>? {
//        assert(!isNullCellAt(position))
        if (buffer.size == 1) { return null }

        val newBuffer = bufferRemoveCellAtIndex(cellIndex)
        return TrieNode(bitmap xor position, newBuffer)
    }

    private fun mutableRemoveCellAtIndex(cellIndex: Int, position: Int, mutatorMarker: Marker): TrieNode<E>? {
//        assert(!isNullCellAt(position))
        if (buffer.size == 1) { return null }

        if (marker === mutatorMarker) {
            buffer = bufferRemoveCellAtIndex(cellIndex)
            bitmap = bitmap xor position
            return this
        }
        val newBuffer = bufferRemoveCellAtIndex(cellIndex)
        return TrieNode(bitmap xor position, newBuffer, mutatorMarker)
    }

    private fun collisionRemoveElementAtIndex(i: Int): TrieNode<E>? {
        if (buffer.size == 1) { return null }

        val newBuffer = bufferRemoveCellAtIndex(i)
        return TrieNode(0, newBuffer)
    }

    private fun mutableCollisionRemoveElementAtIndex(i: Int, mutatorMarker: Marker): TrieNode<E>? {
        if (buffer.size == 1) { return null }

        if (marker === mutatorMarker) {
            buffer = bufferRemoveCellAtIndex(i)
            return this
        }
        val newBuffer = bufferRemoveCellAtIndex(i)
        return TrieNode(0, newBuffer, mutatorMarker)
    }

    private fun collisionContainsElement(element: E): Boolean {
        return buffer.contains(element)
    }

    private fun collisionAdd(element: E): TrieNode<E> {
        if (collisionContainsElement(element)) { return this }
        val newBuffer = bufferAddElementAtIndex(0, element)
        return TrieNode(0, newBuffer)
    }

    private fun mutableCollisionAdd(element: E, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        if (collisionContainsElement(element)) { return this }
        mutator.size++
        if (marker === mutator.marker) {
            buffer = bufferAddElementAtIndex(0, element)
            return this
        }
        val newBuffer = bufferAddElementAtIndex(0, element)
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
        val cellPosition = 1 shl ((elementHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (isNullCellAt(cellPosition)) { // element is absent
            return false
        }

        val cellIndex = indexOfCellAt(cellPosition)
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
        val cellPosition = 1 shl ((elementHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (isNullCellAt(cellPosition)) { // element is absent
            return addElementAt(cellPosition, element)
        }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionAdd(element)
            } else {
                targetNode.add(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            return updateNodeAtIndex(cellIndex, newNode)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) { return this }
        return moveElementToNode(cellIndex, elementHash, element, shift)
    }

    fun mutableAdd(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E> {
        val cellPosition = 1 shl ((elementHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (isNullCellAt(cellPosition)) { // element is absent
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
            if (targetNode === newNode) { return this }
            return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.marker)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) { return this }
        mutator.size++
        return mutableMoveElementToNode(cellIndex, elementHash, element, shift, mutator.marker)
    }

    fun remove(elementHash: Int, element: E, shift: Int): TrieNode<E>? {
        val cellPosition = 1 shl ((elementHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (isNullCellAt(cellPosition)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.collisionRemove(element)
            } else {
                targetNode.remove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeCellAtIndex(cellIndex, cellPosition) }
            return updateNodeAtIndex(cellIndex, newNode)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            return removeCellAtIndex(cellIndex, cellPosition)
        }
        return this
    }

    fun mutableRemove(elementHash: Int, element: E, shift: Int, mutator: PersistentHashSetBuilder<*>): TrieNode<E>? {
        val cellPosition = 1 shl ((elementHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (isNullCellAt(cellPosition)) { // element is absent
            return this
        }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is TrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            val newNode = if (shift == MAX_SHIFT) {
                targetNode.mutableCollisionRemove(element, mutator)
            } else {
                targetNode.mutableRemove(elementHash, element, shift + LOG_MAX_BRANCHING_FACTOR, mutator)
            }
            if (targetNode === newNode) { return this }
            if (newNode == null) { return mutableRemoveCellAtIndex(cellIndex, cellPosition, mutator.marker) }
            return mutableUpdateNodeAtIndex(cellIndex, newNode, mutator.marker)
        }
        // element is directly in buffer
        if (element == buffer[cellIndex]) {
            mutator.size--
            return mutableRemoveCellAtIndex(cellIndex, cellPosition, mutator.marker)   // check is empty
        }
        return this
    }

    internal companion object {
        internal val EMPTY = TrieNode<Nothing>(0, emptyArray())
    }
}