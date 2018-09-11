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

package kotlinx.collections.immutable.implementations.immutableList

import kotlinx.collections.immutable.PersistentList

internal class PersistentVector<E>(private val root: Array<Any?>,
                                   private val tail: Array<Any?>,
                                   override val size: Int,
                                   private val shiftStart: Int) : PersistentList<E>, AbstractPersistentList<E>() {
    private fun rootSize(): Int {
        return ((size - 1) shr LOG_MAX_BUFFER_SIZE) shl LOG_MAX_BUFFER_SIZE
    }

    private fun bufferWith(e: Any?): Array<Any?> {
        val buffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
        buffer[0] = e
        return buffer
    }

    override fun add(element: E): PersistentList<E> {
        val tailSize = size - rootSize()
        if (tailSize < MAX_BUFFER_SIZE) {
            val newTail = tail.copyOf(MAX_BUFFER_SIZE)
            newTail[tailSize] = element
            return PersistentVector(root, newTail, size + 1, shiftStart)
        }

        val newTail = bufferWith(element)
        return pushFullTail(root, tail, newTail)
    }

    private fun pushFullTail(root: Array<Any?>, fullTail: Array<Any?>, newTail: Array<Any?>): PersistentVector<E> {
        if (size shr LOG_MAX_BUFFER_SIZE > 1 shl shiftStart) {
            var newRoot = bufferWith(root)
            newRoot = pushTail(shiftStart + LOG_MAX_BUFFER_SIZE, newRoot, fullTail)
            return PersistentVector(newRoot, newTail, size + 1, shiftStart + LOG_MAX_BUFFER_SIZE)
        }

        val newRoot = pushTail(shiftStart, root, fullTail)
        return PersistentVector(newRoot, newTail, size + 1, shiftStart)
    }

    override fun add(index: Int, element: E): PersistentList<E> {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException()
        }
        if (index == size) {
            return add(element)
        }

        val rootSize = rootSize()
        if (index >= rootSize) {
            return addToTail(root, index - rootSize, element)
        }

        val lastElementWrapper = ObjectWrapper(null)
        val newRoot = addToRoot(root, shiftStart, index, element, lastElementWrapper)
        return addToTail(newRoot, 0, lastElementWrapper.value)
    }

    private fun addToTail(root: Array<Any?>, index: Int, element: Any?): PersistentVector<E> {
        val tailFilledSize = size - rootSize()
        val newTail = tail.copyOf(MAX_BUFFER_SIZE)
        if (tailFilledSize < MAX_BUFFER_SIZE) {
            System.arraycopy(tail, index, newTail, index + 1, tailFilledSize - index)
            newTail[index] = element
            return PersistentVector(root, newTail, size + 1, shiftStart)
        }

        val lastElement = tail[MAX_BUFFER_SIZE_MINUS_ONE]
        System.arraycopy(tail, index, newTail, index + 1, tailFilledSize - index - 1)
        newTail[index] = element
        return pushFullTail(root, newTail, bufferWith(lastElement))
    }

    private fun addToRoot(root: Array<Any?>, shift: Int, index: Int, element: Any?, lastWrapper: ObjectWrapper): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == 0) {
            lastWrapper.value = root[MAX_BUFFER_SIZE_MINUS_ONE]
            val newRoot = if (bufferIndex == 0) arrayOfNulls<Any?>(MAX_BUFFER_SIZE) else root.copyOf(MAX_BUFFER_SIZE)
            System.arraycopy(root, bufferIndex, newRoot, bufferIndex + 1, MAX_BUFFER_SIZE - bufferIndex - 1)
            newRoot[bufferIndex] = element
            return newRoot
        }

        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        newRoot[bufferIndex] = addToRoot(root[bufferIndex] as Array<Any?>,
                shift - LOG_MAX_BUFFER_SIZE, index, element, lastWrapper)

        for (i in bufferIndex + 1 until MAX_BUFFER_SIZE) {
            if (newRoot[i] == null) { break }
            newRoot[i] = addToRoot(root[i] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, 0, lastWrapper.value, lastWrapper)
        }

        return newRoot
    }

    override fun removeAt(index: Int): PersistentList<E> {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        val rootSize = rootSize()
        if (index >= rootSize) {
            return removeFromTail(root, rootSize, shiftStart, index - rootSize)
        }
        val tailElementWrapper = ObjectWrapper(tail[0])
        val newRoot = removeFromRoot(root, shiftStart, index, tailElementWrapper)
        return removeFromTail(newRoot, rootSize, shiftStart, 0)
    }

    private fun pullLastBufferFromRoot(root: Array<Any?>, rootSize: Int, shift: Int): PersistentList<E> {
        if (shift == 0) {
            return SmallPersistentVector(root)
        }
        val lastBufferWrapper = ObjectWrapper(null)
        val newRoot = pullLastBuffer(root, shift, rootSize - 1, lastBufferWrapper)!!
        val newTail = lastBufferWrapper.value as Array<Any?>

        if (newRoot[1] == null) {
            return PersistentVector(newRoot[0] as Array<Any?>, newTail, rootSize, shift - LOG_MAX_BUFFER_SIZE)
        }
        return PersistentVector(newRoot, newTail, rootSize, shift)
    }

    private fun pullLastBuffer(root: Array<Any?>, shift: Int, index: Int, tailWrapper: ObjectWrapper): Array<Any?>? {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == LOG_MAX_BUFFER_SIZE) {
            tailWrapper.value = root[bufferIndex]
            if (bufferIndex == 0) {
                return null
            }
            val newRoot = root.copyOf(MAX_BUFFER_SIZE)
            newRoot[bufferIndex] = null
            return newRoot
        }
        val bufferAtIndex = pullLastBuffer(root[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, tailWrapper)
        if (bufferAtIndex == null && bufferIndex == 0) {
            return null
        }
        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        newRoot[bufferIndex] = bufferAtIndex
        return newRoot
    }

    private fun removeFromTail(root: Array<Any?>, rootSize: Int, shift: Int, index: Int): PersistentList<E> {
        val tailFilledSize = size - rootSize
        assert(index < tailFilledSize)

        if (tailFilledSize == 1) {
            return pullLastBufferFromRoot(root, rootSize, shift)
        }
        val newTail = tail.copyOf(MAX_BUFFER_SIZE)
        if (index < tailFilledSize - 1) {
            System.arraycopy(tail, index + 1, newTail, index, tailFilledSize - index - 1)
        }
        newTail[tailFilledSize - 1] = null
        return PersistentVector(root, newTail, rootSize + tailFilledSize - 1, shift)
    }

    private fun removeFromRoot(root: Array<Any?>, shift: Int, index: Int, tailWrapper: ObjectWrapper): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == 0) {
            val newRoot = if (bufferIndex == 0) arrayOfNulls<Any?>(MAX_BUFFER_SIZE) else root.copyOf(MAX_BUFFER_SIZE)
            System.arraycopy(root, bufferIndex + 1, newRoot, bufferIndex, MAX_BUFFER_SIZE - bufferIndex - 1)
            newRoot[MAX_BUFFER_SIZE - 1] = tailWrapper.value
            tailWrapper.value = root[0]
            return newRoot
        }

        var bufferLastIndex = MAX_BUFFER_SIZE_MINUS_ONE
        while (root[bufferLastIndex] == null) {
            bufferLastIndex -= 1
        }

        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        for (i in bufferLastIndex downTo bufferIndex + 1) {
            newRoot[i] = removeFromRoot(newRoot[i] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, 0, tailWrapper)
        }
        newRoot[bufferIndex] =
                removeFromRoot(newRoot[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, tailWrapper)

        return newRoot
    }

    override fun builder(): PersistentList.Builder<E> {
        return PersistentVectorBuilder(this, root, tail, shiftStart)
    }

    override fun listIterator(index: Int): ListIterator<E> {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException()
        }
        return PersistentVectorIterator(root, tail as Array<E>, index, size, shiftStart / LOG_MAX_BUFFER_SIZE + 1)
    }

    private fun pushTail(shift: Int, root: Array<Any?>?, tail: Array<Any?>): Array<Any?> {
        val index = ((size - 1) shr shift) and MAX_BUFFER_SIZE_MINUS_ONE
        val newRootNode = root?.copyOf(MAX_BUFFER_SIZE) ?: arrayOfNulls<Any?>(MAX_BUFFER_SIZE)

        if (shift == LOG_MAX_BUFFER_SIZE) {
            newRootNode[index] = tail
        } else {
            newRootNode[index] = pushTail(shift - LOG_MAX_BUFFER_SIZE, newRootNode[index] as Array<Any?>?, tail)
        }
        return newRootNode
    }

    private fun bufferFor(index: Int): Array<Any?> {
        if (rootSize() <= index) {
            return tail
        }
        var buffer = root
        var shift = shiftStart
        while (shift > 0) {
            buffer = buffer[(index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE] as Array<Any?>
            shift -= LOG_MAX_BUFFER_SIZE
        }
        return buffer
    }

    override fun get(index: Int): E {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        val buffer = bufferFor(index)
        return buffer[index and MAX_BUFFER_SIZE_MINUS_ONE] as E
    }

    override fun set(index: Int, element: E): PersistentList<E> {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        if (rootSize() <= index) {
            val newTail = tail.copyOf(MAX_BUFFER_SIZE)
            newTail[index and MAX_BUFFER_SIZE_MINUS_ONE] = element
            return PersistentVector(root, newTail, size, shiftStart)
        }

        val newRoot = setInRoot(root, shiftStart, index, element)
        return PersistentVector(newRoot, tail, size, shiftStart)
    }

    private fun setInRoot(root: Array<Any?>, shift: Int, index: Int, e: Any?): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE
        val newRoot = root.copyOf(MAX_BUFFER_SIZE)
        if (shift == 0) {
            newRoot[bufferIndex] = e
        } else {
            newRoot[bufferIndex] = setInRoot(newRoot[bufferIndex] as Array<Any?>,
                    shift - LOG_MAX_BUFFER_SIZE, index, e)
        }
        return newRoot
    }
}