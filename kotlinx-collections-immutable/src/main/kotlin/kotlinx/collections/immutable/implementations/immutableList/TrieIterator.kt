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

internal class TrieIterator<out E>(root: Array<Any?>,
                                   index: Int,
                                   size: Int,
                                   private val height: Int) : AbstractListIterator<E>(index, size) {
    private val path: Array<Any?> = arrayOfNulls<Any?>(height)
    private var isInRightEdge = index == size

    init {
        path[0] = root
        fillPath(index - if (isInRightEdge) 1 else 0, 1)
    }

    private fun fillPath(index: Int, startLevel: Int) {
        var shift = (height - startLevel) * LOG_MAX_BUFFER_SIZE
        var i = startLevel
        while (i < height) {
            path[i] = (path[i - 1] as Array<Any?>)[indexAtShift(index, shift)]
            shift -= LOG_MAX_BUFFER_SIZE
            i += 1
        }
    }

    private fun fillPathIfNeeded(indexPredicate: Int) {
        var shift = 0
        while (indexAtShift(index, shift) == indexPredicate) {
            shift += LOG_MAX_BUFFER_SIZE
        }

        if (shift > 0) {
            val level = height - 1 - shift / LOG_MAX_BUFFER_SIZE
            fillPath(index, level + 1)
        }
    }

    private fun elementAtCurrentIndex(): E {
        val leafBufferIndex = index and MAX_BUFFER_SIZE_MINUS_ONE
        return (path[height - 1] as Array<E>)[leafBufferIndex]
    }

    private fun indexAtShift(index: Int, shift: Int): Int {
        return (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE
    }

    override fun next(): E {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val result = elementAtCurrentIndex()
        index += 1

        if (index == size) {
            isInRightEdge = true
            return result
        }

        fillPathIfNeeded(0)

        return result
    }

    override fun previous(): E {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }

        index -= 1

        if (isInRightEdge) {
            isInRightEdge = false
            return elementAtCurrentIndex()
        }

        fillPathIfNeeded(MAX_BUFFER_SIZE_MINUS_ONE)

        return elementAtCurrentIndex()
    }
}