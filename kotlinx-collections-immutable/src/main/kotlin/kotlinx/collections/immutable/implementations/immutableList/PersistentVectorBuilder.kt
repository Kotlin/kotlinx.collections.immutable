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
import kotlinx.collections.immutable.internal.ListImplementation.checkElementIndex
import kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex

private class Marker // TODO: Rename to MutabilityOwnership?

class PersistentVectorBuilder<E>(private var vector: PersistentList<E>,
                                 private var vectorRoot: Array<Any?>?,
                                 private var vectorTail: Array<Any?>,
                                 internal var rootShift: Int) : AbstractMutableList<E>(), PersistentList.Builder<E> {
    private var marker = Marker()
    internal var root = vectorRoot
        private set
    internal var tail = vectorTail
        private set
    override var size = vector.size
        private set

    internal fun getModCount() = modCount

    override fun build(): PersistentList<E> {
        vector = if (root === vectorRoot && tail === vectorTail) {
            vector
        } else {
            marker = Marker()
            vectorRoot = root
            vectorTail = tail
            if (root == null) {
                if (tail.isEmpty()) {
                    persistentVectorOf()
                } else {
                    SmallPersistentVector(tail.copyOf(size))
                }
            } else {
                PersistentVector(root!!, tail, size, rootShift)
            }
        }
        return vector
    }

    private fun rootSize(): Int {
        if (size <= MAX_BUFFER_SIZE) {
            return 0
        }
        return rootSize(size)
    }

    /**
     * Checks if [buffer] is mutable and returns it or its mutable copy.
     */
    private fun makeMutable(buffer: Array<Any?>?): Array<Any?> {
        if (buffer == null) {
            val newBuffer = arrayOfNulls<Any?>(MUTABLE_BUFFER_SIZE)
            newBuffer[MUTABLE_BUFFER_SIZE - 1] = marker
            return newBuffer
        }
        if (buffer.size != MUTABLE_BUFFER_SIZE || buffer[MUTABLE_BUFFER_SIZE - 1] !== marker) {
            val newBuffer = arrayOfNulls<Any?>(MUTABLE_BUFFER_SIZE)
            buffer.copyInto(newBuffer, endIndex = buffer.size.coerceAtMost(MAX_BUFFER_SIZE))
            newBuffer[MUTABLE_BUFFER_SIZE - 1] = marker
            return newBuffer
        }
        return buffer
    }

    private fun mutableBufferWith(element: Any?): Array<Any?> {
        val buffer = arrayOfNulls<Any?>(MUTABLE_BUFFER_SIZE)
        buffer[0] = element
        buffer[MUTABLE_BUFFER_SIZE - 1] = marker
        return buffer
    }

    override fun add(element: E): Boolean {
        modCount += 1

        val tailSize = size - rootSize()
        if (tailSize < MAX_BUFFER_SIZE) {
            val mutableTail = makeMutable(tail)
            mutableTail[tailSize] = element
            this.tail = mutableTail
            this.size += 1
        } else {
            val newTail = mutableBufferWith(element)
            this.pushFilledTail(root, tail, newTail)
        }
        return true
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     */
    private fun pushFilledTail(root: Array<Any?>?, filledTail: Array<Any?>, newTail: Array<Any?>) = when {
        size shr LOG_MAX_BUFFER_SIZE > 1 shl rootShift -> {
            // if the root trie is filled entirely, promote it to the next level
            this.root = pushTail(mutableBufferWith(root), filledTail, rootShift + LOG_MAX_BUFFER_SIZE)
            this.tail = newTail
            this.rootShift += LOG_MAX_BUFFER_SIZE
            this.size += 1
        }
        root == null -> {
            this.root = filledTail
            this.tail = newTail
            this.size += 1
        }
        else -> {
            this.root = pushTail(root, filledTail, rootShift)
            this.tail = newTail
            this.size += 1
        }
    }

    /**
     * Appends the specified entirely filled [tail] as a leaf buffer to the next free position in the [root] trie.
     * The trie must not be filled entirely.
     */
    private fun pushTail(root: Array<Any?>?, tail: Array<Any?>, shift: Int): Array<Any?> {
        val index = indexSegment(size - 1, shift)
        val mutableRoot = makeMutable(root)

        if (shift == LOG_MAX_BUFFER_SIZE) {
            mutableRoot[index] = tail
        } else {
            @Suppress("UNCHECKED_CAST")
            mutableRoot[index] = pushTail(mutableRoot[index] as Array<Any?>?, tail, shift - LOG_MAX_BUFFER_SIZE)
        }
        return mutableRoot
    }

    override fun add(index: Int, element: E) {
        checkPositionIndex(index, size)

        if (index == size) {
            add(element)
            return
        }

        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            insertIntoTail(root, index - rootSize, element)
            return
        }

        val elementCarry = ObjectRef(null)
        val newRest = insertIntoRoot(root!!, rootShift, index, element, elementCarry)
        insertIntoTail(newRest, 0, elementCarry.value as E)
    }


    private fun insertIntoTail(root: Array<Any?>?, index: Int, element: E) {
        val tailSize = size - rootSize()
        val mutableTail = makeMutable(tail)
        if (tailSize < MAX_BUFFER_SIZE) {
            tail.copyInto(mutableTail, index + 1, index, tailSize)
            mutableTail[index] = element
            this.root = root
            this.tail = mutableTail
            this.size += 1
        } else {
            val lastElement = tail[MAX_BUFFER_SIZE_MINUS_ONE]
            tail.copyInto(mutableTail, index + 1, index, MAX_BUFFER_SIZE_MINUS_ONE)
            mutableTail[index] = element
            pushFilledTail(root, mutableTail, mutableBufferWith(lastElement))
        }
    }

    /**
     * Insert the specified [element] into the [root] trie at the specified trie [index].
     *
     * [elementCarry] contains the last element of this trie that was popped out by the insertion operation.
     *
     * @return new root trie or this modified trie, if it's already mutable
     */
    private fun insertIntoRoot(root: Array<Any?>, shift: Int, index: Int, element: Any?, elementCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)

        if (shift == 0) {
            elementCarry.value = root[MAX_BUFFER_SIZE_MINUS_ONE]
            val mutableRoot = makeMutable(root)
            root.copyInto(mutableRoot, bufferIndex + 1, bufferIndex, MAX_BUFFER_SIZE_MINUS_ONE)
            mutableRoot[bufferIndex] = element
            return mutableRoot
        }

        val mutableRoot = makeMutable(root)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                insertIntoRoot(mutableRoot[bufferIndex] as Array<Any?>, lowerLevelShift, index, element, elementCarry)

        for (i in bufferIndex + 1 until MAX_BUFFER_SIZE) {
            if (mutableRoot[i] == null) break
            @Suppress("UNCHECKED_CAST")
            mutableRoot[i] =
                    insertIntoRoot(mutableRoot[i] as Array<Any?>, lowerLevelShift, 0, elementCarry.value, elementCarry)
        }

        return mutableRoot
    }

    override fun get(index: Int): E {
        checkElementIndex(index, size)

        val buffer = bufferFor(index)
        @Suppress("UNCHECKED_CAST")
        return buffer[index and MAX_BUFFER_SIZE_MINUS_ONE] as E
    }

    private fun bufferFor(index: Int): Array<Any?> {
        if (rootSize() <= index) {
            return tail
        }
        var buffer = root!!
        var shift = rootShift
        while (shift > 0) {
            @Suppress("UNCHECKED_CAST")
            buffer = buffer[indexSegment(index, shift)] as Array<Any?>
            shift -= LOG_MAX_BUFFER_SIZE
        }
        return buffer
    }

    override fun removeAt(index: Int): E {
        checkElementIndex(index, size)

        modCount += 1

        val rootSize = rootSize()
        if (index >= rootSize) {
            @Suppress("UNCHECKED_CAST")
            return removeFromTailAt(root, rootSize, rootShift, index - rootSize) as E
        }
        val elementCarry = ObjectRef(tail[0])
        val newRoot = removeFromRootAt(root!!, rootShift, index, elementCarry)
        removeFromTailAt(newRoot, rootSize, rootShift, 0)
        @Suppress("UNCHECKED_CAST")
        return elementCarry.value as E
    }

    private fun removeFromTailAt(root: Array<Any?>?, rootSize: Int, shift: Int, index: Int): Any? {
        val tailSize = size - rootSize
        assert(index < tailSize)

        val removedElement: Any?
        if (tailSize == 1) {
            removedElement = tail[0]
            pullLastBufferFromRoot(root, rootSize, shift)
        } else {
            removedElement = tail[index]
            val mutableTail = makeMutable(tail)
            tail.copyInto(mutableTail, index, index + 1, tailSize)
            mutableTail[tailSize - 1] = null
            this.root = root
            this.tail = mutableTail
            this.size = rootSize + tailSize - 1
            this.rootShift = shift
        }
        return removedElement
    }

    /**
     * Removes element from trie at the specified trie [index].
     *
     * [tailCarry] on input contains the first element of the adjacent trie to fill the last vacant element with.
     * [tailCarry] on output contains the first element of this trie.
     *
     * @return the new root of the trie.
     */
    private fun removeFromRootAt(root: Array<Any?>, shift: Int, index: Int, tailCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)

        if (shift == 0) {
            val removedElement = root[bufferIndex]
            val mutableRoot = makeMutable(root)
            root.copyInto(mutableRoot, bufferIndex, bufferIndex + 1, MAX_BUFFER_SIZE)
            mutableRoot[MAX_BUFFER_SIZE - 1] = tailCarry.value
            tailCarry.value = removedElement
            return mutableRoot
        }

        var bufferLastIndex = MAX_BUFFER_SIZE_MINUS_ONE
        if (root[bufferLastIndex] == null) {
            bufferLastIndex = indexSegment(rootSize() - 1, shift)
        }

        val mutableRoot = makeMutable(root)
        val lowerLevelShift = shift - LOG_MAX_BUFFER_SIZE

        for (i in bufferLastIndex downTo bufferIndex + 1) {
            @Suppress("UNCHECKED_CAST")
            mutableRoot[i] = removeFromRootAt(mutableRoot[i] as Array<Any?>, lowerLevelShift, 0, tailCarry)
        }
        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                removeFromRootAt(mutableRoot[bufferIndex] as Array<Any?>, lowerLevelShift, index, tailCarry)

        return mutableRoot
    }

    /**
     * Extracts the last entirely filled leaf buffer from the trie of this vector and makes it a tail in this
     *
     * Used when there are no elements left in current tail.
     *
     * Requires the trie to contain at least one leaf buffer.
     */
    private fun pullLastBufferFromRoot(root: Array<Any?>?, rootSize: Int, shift: Int) {
        if (shift == 0) {
            this.root = null
            this.tail = root ?: emptyArray()
            this.size = rootSize
            this.rootShift = shift
            return
        }

        val tailCarry = ObjectRef(null)
        val newRoot = pullLastBuffer(root!!, shift, rootSize, tailCarry)!!
        @Suppress("UNCHECKED_CAST")
        this.tail = tailCarry.value as Array<Any?>
        this.size = rootSize

        // check if the new root contains only one element
        if (newRoot[1] == null) {
            // demote the root trie to the lower level
            @Suppress("UNCHECKED_CAST")
            this.root = newRoot[0] as Array<Any?>?
            this.rootShift = shift - LOG_MAX_BUFFER_SIZE
        } else {
            this.root = newRoot
            this.rootShift = shift
        }
    }

    /**
     * Extracts the last leaf buffer from trie and returns new trie without it or `null` if there's no more leaf elements in this trie.
     *
     * [tailCarry] on output contains the extracted leaf buffer.
     */
    private fun pullLastBuffer(root: Array<Any?>, shift: Int, rootSize: Int, tailCarry: ObjectRef): Array<Any?>? {
        val bufferIndex = indexSegment(rootSize - 1, shift)

        val newBufferAtIndex = if (shift == LOG_MAX_BUFFER_SIZE) {
            tailCarry.value = root[bufferIndex]
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            pullLastBuffer(root[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, rootSize, tailCarry)
        }
        if (newBufferAtIndex == null && bufferIndex == 0) {
            return null
        }

        val mutableRoot = makeMutable(root)
        mutableRoot[bufferIndex] = newBufferAtIndex
        return mutableRoot
    }

    override fun set(index: Int, element: E): E {
        // TODO: Should list[i] = list[i] make it mutable?
        checkElementIndex(index, size)
        if (rootSize() <= index) {
            val mutableTail = makeMutable(tail)
            val tailIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
            val oldElement = mutableTail[tailIndex]
            mutableTail[tailIndex] = element
            this.tail = mutableTail
            @Suppress("UNCHECKED_CAST")
            return oldElement as E
        }

        val oldElementCarry = ObjectRef(null)
        this.root = setInRoot(root!!, rootShift, index, element, oldElementCarry)
        @Suppress("UNCHECKED_CAST")
        return oldElementCarry.value as E
    }

    private fun setInRoot(root: Array<Any?>, shift: Int, index: Int, e: E, oldElementCarry: ObjectRef): Array<Any?> {
        val bufferIndex = indexSegment(index, shift)
        val mutableRoot = makeMutable(root)

        if (shift == 0) {
            oldElementCarry.value = mutableRoot[bufferIndex]
            mutableRoot[bufferIndex] = e
            return mutableRoot
        }
        @Suppress("UNCHECKED_CAST")
        mutableRoot[bufferIndex] =
                setInRoot(mutableRoot[bufferIndex] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, index, e, oldElementCarry)
        return mutableRoot
    }

    override fun iterator(): MutableIterator<E> {
        return this.listIterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return this.listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkPositionIndex(index, size)
        return PersistentVectorMutableIterator(this, index)
    }
}
