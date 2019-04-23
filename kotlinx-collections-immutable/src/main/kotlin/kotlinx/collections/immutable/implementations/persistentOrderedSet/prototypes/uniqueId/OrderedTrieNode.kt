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

package kotlinx.collections.immutable.implementations.persistentOrderedSet.prototypes.uniqueId

internal const val MAX_BRANCHING_FACTOR = 32
internal const val LOG_MAX_BRANCHING_FACTOR = 5
internal const val MAX_BRANCHING_FACTOR_MINUS_ONE = MAX_BRANCHING_FACTOR - 1
internal const val MAX_SHIFT = 30


internal class OrderedTrieNode<E>(var bitmap: Int,
                                  var buffer: Array<Any?>) {

    constructor(node: OrderedTrieNode<E>): this(1, arrayOf(node))

    private fun isNullCellAt(position: Int): Boolean {
        return bitmap and position == 0
    }

    private fun indexOfCellAt(position: Int): Int {
        return Integer.bitCount(bitmap and (position - 1))
    }

    private fun nodeAtIndex(index: Int): OrderedTrieNode<E> {
        return buffer[index] as OrderedTrieNode<E>
    }

    private fun bufferAddElementAtIndex(index: Int, element: E): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 1)
        System.arraycopy(buffer, 0, newBuffer, 0, index)
        System.arraycopy(buffer, index, newBuffer, index + 1, buffer.size - index)
        newBuffer[index] = element
        return newBuffer
    }

    private fun bufferAddNodeAtIndex(index: Int, node: OrderedTrieNode<E>): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size + 1)
        System.arraycopy(buffer, 0, newBuffer, 0, index)
        System.arraycopy(buffer, index, newBuffer, index + 1, buffer.size - index)
        newBuffer[index] = node
        return newBuffer
    }

    private fun addElementAt(position: Int, element: E): OrderedTrieNode<E> {
//        assert(isNullCellAt(position))

        val index = indexOfCellAt(position)
        val newBuffer = bufferAddElementAtIndex(index, element)
        return OrderedTrieNode(bitmap or position, newBuffer)
    }

    private fun mutableAddElementAt(position: Int, element: E): OrderedTrieNode<E> {
//        assert(isNullCellAt(position))

        val index = indexOfCellAt(position)
        buffer = bufferAddElementAtIndex(index, element)
        bitmap = bitmap or position
        return this
    }

    private fun addNodeAt(position: Int, node: OrderedTrieNode<E>): OrderedTrieNode<E> {
//        assert(isNullCellAt(position))

        val index = indexOfCellAt(position)
        val newBuffer = bufferAddNodeAtIndex(index, node)
        return OrderedTrieNode(bitmap or position, newBuffer)
    }

    private fun updateNodeAtIndex(nodeIndex: Int, newNode: OrderedTrieNode<E>): OrderedTrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        val newBuffer = buffer.copyOf()
        newBuffer[nodeIndex] = newNode
        return OrderedTrieNode(bitmap, newBuffer)
    }

    private fun mutableUpdateNodeAtIndex(nodeIndex: Int, newNode: OrderedTrieNode<E>): OrderedTrieNode<E> {
//        assert(buffer[nodeIndex] !== newNode)

        buffer[nodeIndex] = newNode
        return this
    }

    private fun bufferRemoveCellAtIndex(cellIndex: Int): Array<Any?> {
        val newBuffer = arrayOfNulls<Any?>(buffer.size - 1)
        System.arraycopy(buffer, 0, newBuffer, 0, cellIndex)
        System.arraycopy(buffer, cellIndex + 1, newBuffer, cellIndex, buffer.size - cellIndex - 1)
        return newBuffer
    }

    private fun removeCellAtIndex(cellIndex: Int, position: Int): OrderedTrieNode<E>? {
//        assert(!isNullCellAt(position))
        if (buffer.size == 1) { return null }

        val newBuffer = bufferRemoveCellAtIndex(cellIndex)
        return OrderedTrieNode(bitmap xor position, newBuffer)
    }

    private fun mutableRemoveCellAtIndex(cellIndex: Int, position: Int): OrderedTrieNode<E>? {
//        assert(!isNullCellAt(position))
        if (buffer.size == 1) { return null }

        buffer = bufferRemoveCellAtIndex(cellIndex)
        bitmap = bitmap xor position
        return this
    }

    fun pushTail(id: Int, shift: Int, tail: OrderedTrieNode<E>): OrderedTrieNode<E> {
        val tailPosition = 1 shl ((id ushr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (shift == LOG_MAX_BRANCHING_FACTOR) {
            return addNodeAt(tailPosition, tail)
        }

        return if (isNullCellAt(tailPosition)) {
            val emptyNode = emptyOf<E>()
            val newNode = emptyNode.pushTail(id, shift - LOG_MAX_BRANCHING_FACTOR, tail)
            addNodeAt(tailPosition, newNode)
        } else {
            val tailIndex = indexOfCellAt(tailPosition)
            val newNode = nodeAtIndex(tailIndex).pushTail(id, shift - LOG_MAX_BRANCHING_FACTOR, tail)
            updateNodeAtIndex(tailIndex, newNode)
        }
    }

    fun add(elementId: Int, element: E): OrderedTrieNode<E> {
        val cellPosition = 1 shl (elementId and MAX_BRANCHING_FACTOR_MINUS_ONE)
        assert(isNullCellAt(cellPosition)) { "id should be unique: ${elementId - Int.MIN_VALUE}" }
        return addElementAt(cellPosition, element)
    }

    fun mutableAdd(elementId: Int, element: E, shift: Int): OrderedTrieNode<E> {
        val cellPosition = 1 shl (elementId and MAX_BRANCHING_FACTOR_MINUS_ONE)
        assert(isNullCellAt(cellPosition)) { "id should be unique" }
        return mutableAddElementAt(cellPosition, element)
    }

    fun remove(elementId: Int, element: E, shift: Int): OrderedTrieNode<E>? {
        val cellPosition = 1 shl ((elementId ushr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        assert(!isNullCellAt(cellPosition)) { "Element with specified id doesn't exist" }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is OrderedTrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            if (shift == 0) {
                assert(false) { "Unexpected collision" }
            }
            val newNode = targetNode.remove(elementId, element, shift - LOG_MAX_BRANCHING_FACTOR)
            if (targetNode === newNode) { return this }
            if (newNode == null) { return removeCellAtIndex(cellIndex, cellPosition) }
            return updateNodeAtIndex(cellIndex, newNode)
        }

        assert(element == buffer[cellIndex]) { "id should be unique" }

        return removeCellAtIndex(cellIndex, cellPosition) // checks if empty
    }

    fun mutableRemove(elementId: Int, element: E, shift: Int): OrderedTrieNode<E>? {
        val cellPosition = 1 shl ((elementId ushr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        assert(!isNullCellAt(cellPosition)) { "Element with specified id doesn't exist" }

        val cellIndex = indexOfCellAt(cellPosition)
        if (buffer[cellIndex] is OrderedTrieNode<*>) { // element may be in node
            val targetNode = nodeAtIndex(cellIndex)
            if (shift == 0) {
                assert(false) { "Unexpected collision" }
            }
            val newNode = targetNode.mutableRemove(elementId, element, shift - LOG_MAX_BRANCHING_FACTOR)
            if (targetNode === newNode) { return this }
            if (newNode == null) { return mutableRemoveCellAtIndex(cellIndex, cellPosition) }
            return mutableUpdateNodeAtIndex(cellIndex, newNode)
        }

        assert(element == buffer[cellIndex]) { "id should be unique" }

        return mutableRemoveCellAtIndex(cellIndex, cellPosition) // checks if empty
    }

    internal companion object {
        internal val EMPTY = OrderedTrieNode<Nothing>(0, emptyArray())
        internal fun <E> emptyOf(): OrderedTrieNode<E> = EMPTY as OrderedTrieNode<E>
    }
}