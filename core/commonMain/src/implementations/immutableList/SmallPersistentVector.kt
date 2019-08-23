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

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.internal.ListImplementation.checkElementIndex
import kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex
import kotlinx.collections.immutable.mutate

internal class SmallPersistentVector<E>(private val buffer: Array<Any?>) : ImmutableList<E>, AbstractPersistentList<E>() {

    init {
        assert(buffer.size <= MAX_BUFFER_SIZE)
    }

    override val size: Int
        get() = buffer.size

    private fun bufferOfSize(size: Int): Array<Any?> {
        return arrayOfNulls<Any?>(size)
    }

    override fun add(element: E): PersistentList<E> {
        if (size < MAX_BUFFER_SIZE) {
            val newBuffer = buffer.copyOf(size + 1)
            newBuffer[size] = element
            return SmallPersistentVector(newBuffer)
        }
        val tail = presizedBufferWith(element)
        return PersistentVector(buffer, tail, size + 1, 0)
    }

    override fun addAll(elements: Collection<E>): PersistentList<E> {
        if (size + elements.size <= MAX_BUFFER_SIZE) {
            val newBuffer = buffer.copyOf(size + elements.size)
            // TODO: investigate performance of elements.toArray + copyInto
            var index = size
            for (element in elements) {
                newBuffer[index++] = element
            }
            return SmallPersistentVector(newBuffer)
        }
        return mutate { it.addAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> {
        val newBuffer = buffer.copyOf()
        var newSize = 0
        for (element in buffer) {
            @Suppress("UNCHECKED_CAST")
            if (!predicate(element as E)) {
                newBuffer[newSize++] = element
            }
        }
        return when (newSize) {
            size -> this
            0 -> EMPTY
            else -> SmallPersistentVector(newBuffer.copyOfRange(0, newSize))
        }
    }

    override fun addAll(index: Int, c: Collection<E>): PersistentList<E> {
        checkPositionIndex(index, size)
        if (size + c.size <= MAX_BUFFER_SIZE) {
            val newBuffer = bufferOfSize(size + c.size)
            buffer.copyInto(newBuffer, endIndex = index)
            buffer.copyInto(newBuffer, index + c.size, index, size)
            var position = index
            for (element in c) {
                newBuffer[position++] = element
            }
            return SmallPersistentVector(newBuffer)
        }
        return mutate { it.addAll(index, c) }
    }

    override fun add(index: Int, element: E): PersistentList<E> {
        checkPositionIndex(index, size)
        if (index == size) {
            return add(element)
        }

        if (size < MAX_BUFFER_SIZE) {
            // TODO: copyOf + one copyInto?
            val newBuffer = bufferOfSize(size + 1)
            buffer.copyInto(newBuffer, endIndex = index)
            buffer.copyInto(newBuffer, index + 1, index, size)
            newBuffer[index] = element
            return SmallPersistentVector(newBuffer)
        }

        val root = buffer.copyOf()
        buffer.copyInto(root, index + 1, index, size - 1)
        root[index] = element
        val tail = presizedBufferWith(buffer[MAX_BUFFER_SIZE_MINUS_ONE])
        return PersistentVector(root, tail, size + 1, 0)
    }

    override fun removeAt(index: Int): PersistentList<E> {
        checkElementIndex(index, size)
        if (size == 1) {
            return EMPTY
        }
        val newBuffer = buffer.copyOf(size - 1)
        buffer.copyInto(newBuffer, index, index + 1, size)
        return SmallPersistentVector(newBuffer)
    }

    override fun builder(): PersistentList.Builder<E> {
        return PersistentVectorBuilder(this, null, buffer, 0)
    }

    override fun indexOf(element: E): Int {
        return buffer.indexOf(element)
    }

    override fun lastIndexOf(element: E): Int {
        return buffer.lastIndexOf(element)
    }

    override fun listIterator(index: Int): ListIterator<E> {
        checkPositionIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return BufferIterator(buffer as Array<E>, index, size)
    }

    override fun get(index: Int): E {
        // TODO: use elementAt(index)?
        checkElementIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return buffer[index] as E
    }

    override fun set(index: Int, element: E): PersistentList<E> {
        checkElementIndex(index, size)
        val newBuffer = buffer.copyOf()
        newBuffer[index] = element
        return SmallPersistentVector(newBuffer)
    }

    companion object {
        val EMPTY = SmallPersistentVector<Nothing>(emptyArray())
    }
}