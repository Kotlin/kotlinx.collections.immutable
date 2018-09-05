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

internal class PersistentVectorIterator<out T>(root: Array<Any?>,
                                               private val tail: Array<T>,
                                               index: Int,
                                               size: Int,
                                               trieHeight: Int) : AbstractListIterator<T>(index, size) {
    private val trieIterator: TrieIterator<T>

    init {
        val trieSize = ((size - 1) shr LOG_MAX_BUFFER_SIZE) shl LOG_MAX_BUFFER_SIZE
        val trieIndex = if (trieSize < index) trieSize else index
        trieIterator = TrieIterator(root, trieIndex, trieSize, trieHeight)
    }

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        if (trieIterator.hasNext()) {
            index++
            return trieIterator.next()
        }
        return tail[index++ - trieIterator.size]
    }

    override fun previous(): T {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        if (index > trieIterator.size) {
            return tail[--index - trieIterator.size]
        }
        index--
        return trieIterator.previous()
    }
}