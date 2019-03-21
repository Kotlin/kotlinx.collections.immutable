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

private class Marker

class PersistentVectorBuilder<E>(private var root: Array<Any?>?,
                                 private var tail: Array<Any?>?,
                                 override var size: Int,
                                 private var shiftStart: Int) : AbstractMutableList<E>(), PersistentList.Builder<E> {
    private var marker = Marker()

    override fun build(): PersistentList<E> {
        marker = Marker()
        if (root == null) {
            if (tail == null) {
                return persistentVectorOf()
            }
            return SmallPersistentVector(tail!!.copyOf(size))
        }
        return PersistentVector(root!!, tail!!, size, shiftStart)
    }

    private fun rootSize(): Int {
        if (size <= MAX_BUFFER_SIZE) {
            return 0
        }
        return ((size - 1) shr LOG_MAX_BUFFER_SIZE) shl LOG_MAX_BUFFER_SIZE
    }

    private fun makeMutable(buffer: Array<Any?>?): Array<Any?> {
        if (buffer == null) {
            val newBuffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE_PlUS_ONE)
            newBuffer[MAX_BUFFER_SIZE] = marker
            return newBuffer
        }
        if (buffer.size != MAX_BUFFER_SIZE_PlUS_ONE || buffer[MAX_BUFFER_SIZE] !== marker) {
            val newBuffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE_PlUS_ONE)
            System.arraycopy(buffer, 0, newBuffer, 0, minOf(buffer.size, MAX_BUFFER_SIZE))
            newBuffer[MAX_BUFFER_SIZE] = marker
            return newBuffer
        }
        return buffer
    }

    private fun mutableBufferWith(element: Any?): Array<Any?> {
        val buffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE_PlUS_ONE)
        buffer[0] = element
        buffer[MAX_BUFFER_SIZE] = marker
        return buffer
    }

    override fun add(element: E): Boolean {
        modCount += 1

        val tailSize = size - rootSize()
        if (tailSize < MAX_BUFFER_SIZE) {
            val mutableLast = makeMutable(tail)
            mutableLast[tailSize] = element
            this.tail = mutableLast
            this.size += 1
        } else {
            val newTail = mutableBufferWith(element)
            this.pushFullTail(root, tail!!, newTail)
        }
        return true
    }

    private fun pushFullTail(root: Array<Any?>?, fullTail: Array<Any?>, newLast: Array<Any?>) = when {
        size shr LOG_MAX_BUFFER_SIZE > 1 shl shiftStart -> {
            var mutableRest = mutableBufferWith(root)
            mutableRest = pushTail(mutableRest, fullTail, shiftStart + LOG_MAX_BUFFER_SIZE)
            this.root = mutableRest
            this.tail = newLast
            this.shiftStart += LOG_MAX_BUFFER_SIZE
            this.size += 1
        }
        root == null -> {
            this.root = fullTail
            this.tail = newLast
            this.size += 1
        }
        else -> {
            val newRest = pushTail(root, fullTail, shiftStart)
            this.root = newRest
            this.tail = newLast
            this.size += 1
        }
    }

    private fun pushTail(root: Array<Any?>?, tail: Array<Any?>, shift: Int): Array<Any?> {
        val index = ((size - 1) shr shift) and MAX_BUFFER_SIZE_MINUS_ONE
        val mutableRest = makeMutable(root)

        if (shift == LOG_MAX_BUFFER_SIZE) {
            mutableRest[index] = tail
        } else {
            mutableRest[index] = pushTail(mutableRest[index] as Array<Any?>?, tail, shift - LOG_MAX_BUFFER_SIZE)
        }
        return mutableRest
    }

    override fun add(index: Int, element: E) {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException()
        }
        if (index == size) {
            add(element)
            return
        }

        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            addToTail(root, index - rootSize, element)
            return
        }

        val lastElementWrapper = ObjectWrapper(null)
        val newRest = addToRoot(root!!, shiftStart, index, element, lastElementWrapper)
        addToTail(newRest, 0, lastElementWrapper.value as E)
    }


    private fun addToTail(root: Array<Any?>?, index: Int, element: E) {
        val tailFilledSize = size - rootSize()
        val mutableTail = makeMutable(tail)
        if (tailFilledSize < MAX_BUFFER_SIZE) {
            System.arraycopy(tail, index, mutableTail, index + 1, tailFilledSize - index)
            mutableTail[index] = element
            this.root = root
            this.tail = mutableTail
            this.size += 1
        } else {
            val lastElement = tail!![MAX_BUFFER_SIZE_MINUS_ONE]
            System.arraycopy(tail, index, mutableTail, index + 1, MAX_BUFFER_SIZE_MINUS_ONE - index)
            mutableTail[index] = element
            pushFullTail(root, mutableTail, mutableBufferWith(lastElement))
        }
    }

    private fun addToRoot(root: Array<Any?>, shift: Int, index: Int, element: Any?, lastWrapper: ObjectWrapper): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == 0) {
            lastWrapper.value = root[MAX_BUFFER_SIZE_MINUS_ONE]
            val mutableRoot = makeMutable(root)
            System.arraycopy(root, bufferIndex, mutableRoot, bufferIndex + 1, MAX_BUFFER_SIZE_MINUS_ONE - bufferIndex)
            mutableRoot[bufferIndex] = element
            return mutableRoot
        }

        val mutableRoot = makeMutable(root)
        mutableRoot[bufferIndex] =
                addToRoot(mutableRoot[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, element, lastWrapper)

        for (i in bufferIndex + 1 until MAX_BUFFER_SIZE) {
            if (mutableRoot[i] == null) { break }
            mutableRoot[i] =
                    addToRoot(mutableRoot[i] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, 0, lastWrapper.value, lastWrapper)
        }

        return mutableRoot
    }

    override fun get(index: Int): E {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        val buffer = bufferFor(index)
        return buffer[index and MAX_BUFFER_SIZE_MINUS_ONE] as  E
    }

    private fun bufferFor(index: Int): Array<Any?> {
        if (rootSize() <= index) {
            return tail!!
        }
        var buffer = root!!
        var shift = shiftStart
        while (shift > 0) {
            buffer = buffer[(index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE] as Array<Any?>
            shift -= LOG_MAX_BUFFER_SIZE
        }
        return buffer
    }

    override fun removeAt(index: Int): E {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            return removeFromTail(root, rootSize, shiftStart, index - rootSize) as E
        }
        val lastElementWrapper = ObjectWrapper(tail!![0])
        val newRoot = removeFromRoot(root!!, shiftStart, index, lastElementWrapper)
        removeFromTail(newRoot, rootSize, shiftStart, 0)
        return lastElementWrapper.value as E
    }

    private fun removeFromTail(root: Array<Any?>?, rootSize: Int, shift: Int, index: Int): Any? {
        val lastFilledSize = size - rootSize
        assert(index < lastFilledSize)

        val removedElement: Any?
        if (lastFilledSize == 1) {
            removedElement = tail!![0]
            pullLastBufferFromRoot(root, rootSize, shift)
        } else {
            removedElement = tail!![index]
            val mutableLast = makeMutable(tail)
            System.arraycopy(tail, index + 1, mutableLast, index, lastFilledSize - index - 1)
            mutableLast[lastFilledSize - 1] = null as E
            this.root = root
            this.tail = mutableLast
            this.size = rootSize + lastFilledSize - 1
            this.shiftStart = shift
        }
        return removedElement
    }

    private fun removeFromRoot(root: Array<Any?>, shift: Int, index: Int, lastWrapper: ObjectWrapper): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == 0) {
            val removedElement = root[bufferIndex]
            val mutableRest = makeMutable(root)
            System.arraycopy(root, bufferIndex + 1, mutableRest, bufferIndex, MAX_BUFFER_SIZE - bufferIndex - 1)
            mutableRest[MAX_BUFFER_SIZE - 1] = lastWrapper.value
            lastWrapper.value = removedElement
            return mutableRest
        }

        var bufferLastIndex = MAX_BUFFER_SIZE_MINUS_ONE
        while (root[bufferLastIndex] == null) {
            bufferLastIndex -= 1
        }

        val mutableRest = makeMutable(root)
        for (i in bufferLastIndex downTo bufferIndex + 1) {
            mutableRest[i] = removeFromRoot(mutableRest[i] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, 0, lastWrapper)
        }
        mutableRest[bufferIndex] =
                removeFromRoot(mutableRest[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, lastWrapper)

        return mutableRest
    }

    private fun pullLastBufferFromRoot(root: Array<Any?>?, rootSize: Int, shift: Int) {
        if (shift == 0) {
            this.root = null
            this.tail = root
            this.size = rootSize
            this.shiftStart = shift
            return
        }

        val lastBufferWrapper = ObjectWrapper(null)
        val newRest = pullLastBuffer(root!!, shift, rootSize, lastBufferWrapper)!!
        this.root = newRest
        this.tail = lastBufferWrapper.value as Array<Any?>
        this.size = rootSize
        this.shiftStart = shift
        if (newRest[1] == null) {
            this.root = newRest[0] as Array<Any?>?
            this.shiftStart -= LOG_MAX_BUFFER_SIZE
        }
    }

    private fun pullLastBuffer(root: Array<Any?>, shift: Int, rootSize: Int, lastWrapper: ObjectWrapper): Array<Any?>? {
        val bufferIndex = ((rootSize - 1) shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

        if (shift == LOG_MAX_BUFFER_SIZE) {
            lastWrapper.value = root[bufferIndex] as Array<Any?>
            if (bufferIndex == 0) {
                return null
            }
            val mutableRoot = makeMutable(root)
            mutableRoot[bufferIndex] = null
            return mutableRoot
        }
        val bufferAtIndex =
                pullLastBuffer(root[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, rootSize, lastWrapper)
        if (bufferAtIndex == null && bufferIndex == 0) {
            return null
        }
        val mutableRoot = makeMutable(root)
        mutableRoot[bufferIndex] = bufferAtIndex
        return mutableRoot
    }

    override fun set(index: Int, element: E): E {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException()
        }
        if (rootSize() <= index) {
            val mutableLast = makeMutable(tail)
            val oldElement = mutableLast[index and MAX_BUFFER_SIZE_MINUS_ONE]
            mutableLast[index and MAX_BUFFER_SIZE_MINUS_ONE] = element
            this.tail = mutableLast
            return oldElement as E
        }

        val oldElement = ObjectWrapper(null)
        this.root = setInRoot(root!!, shiftStart, index, element, oldElement)
        return oldElement.value as E
    }

    private fun setInRoot(root: Array<Any?>, shift: Int, index: Int, e: E, oldElement: ObjectWrapper): Array<Any?> {
        val bufferIndex = (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE
        val mutableRoot = makeMutable(root)

        if (shift == 0) {
            oldElement.value = mutableRoot[bufferIndex]
            mutableRoot[bufferIndex] = e
            return mutableRoot
        }
        mutableRoot[bufferIndex] =
                setInRoot(mutableRoot[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, e, oldElement)
        return mutableRoot
    }
}
