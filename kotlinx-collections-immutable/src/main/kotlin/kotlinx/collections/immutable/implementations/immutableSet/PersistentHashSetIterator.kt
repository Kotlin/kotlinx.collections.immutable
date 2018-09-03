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

internal open class PersistentHashSetIterator<E>(node: TrieNode<E>) : Iterator<E> {
    protected val path = mutableListOf(TrieNodeIterator<E>())
    protected var pathLastIndex = 0
    private var hasNext = true

    init {
        path[0].reset(node.buffer)
        pathLastIndex = 0
        ensureNextElementIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hasNextElement()) {
            return pathIndex
        }
        if (path[pathIndex].hasNextNode()) {
            val node = path[pathIndex].currentNode()

            if (pathIndex + 1 == path.size) {
                path.add(TrieNodeIterator())
            }
            path[pathIndex + 1].reset(node.buffer)
            return moveToNextNodeWithData(pathIndex + 1)
        }
        return -1
    }

    private fun ensureNextElementIsReady() {
        if (path[pathLastIndex].hasNextElement()) {
            return
        }
        for(i in pathLastIndex downTo 0) {
            var result = moveToNextNodeWithData(i)

            if (result == -1 && path[i].hasNextCell()) {
                path[i].moveToNextCell()
                result = moveToNextNodeWithData(i)
            }
            if (result != -1) {
                pathLastIndex = result
                return
            }
            if (i > 0) {
                path[i - 1].moveToNextCell()
            }
        }
        hasNext = false
    }

    override fun hasNext(): Boolean {
        return hasNext
    }

    override fun next(): E {
        assert(hasNext())

        val result = path[pathLastIndex].nextElement()
        ensureNextElementIsReady()
        return result
    }

    protected fun currentElement(): E {
        assert(hasNext())
        return path[pathLastIndex].currentElement()
    }
}

internal class TrieNodeIterator<out E> {
    private var buffer = emptyArray<Any?>()
    private var index = 0

    fun reset(buffer: Array<Any?>, index: Int = 0) {
        this.buffer = buffer
        this.index = index
    }

    fun hasNextCell(): Boolean {
        return index < buffer.size
    }

    fun moveToNextCell() {
        assert(hasNextCell())
        index++
    }

    fun hasNextElement(): Boolean {
        return hasNextCell() && buffer[index] !is TrieNode<*>
    }

    fun currentElement(): E {
        assert(hasNextElement())
        return buffer[index] as E
    }

    fun nextElement(): E {
        assert(hasNextElement())
        return buffer[index++] as E
    }

    fun hasNextNode(): Boolean {
        return hasNextCell() && buffer[index] is TrieNode<*>
    }

    fun currentNode(): TrieNode<out E> {
        assert(hasNextNode())
        return buffer[index] as TrieNode<E>
    }
}
