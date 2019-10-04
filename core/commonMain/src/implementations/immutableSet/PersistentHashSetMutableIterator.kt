/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.internal.assert

internal class PersistentHashSetMutableIterator<E>(private val builder: PersistentHashSetBuilder<E>)
    : PersistentHashSetIterator<E>(builder.node), MutableIterator<E> {
    private var lastIteratedElement: E? = null
    private var nextWasInvoked = false
    private var expectedModCount = builder.modCount

    override fun next(): E {
        checkForComodification()
        val next = super.next()
        lastIteratedElement = next
        nextWasInvoked = true
        return next
    }

    override fun remove() {
        checkNextWasInvoked()
        if (hasNext()) {
            val currentElement = currentElement()

            assert(builder.remove(lastIteratedElement))
            resetPath(currentElement.hashCode(), builder.node, currentElement, 0)
        } else {
            assert(builder.remove(lastIteratedElement))
        }

        lastIteratedElement = null
        nextWasInvoked = false
        expectedModCount = builder.modCount
    }

    private fun resetPath(hashCode: Int, node: TrieNode<*>, element: E, pathIndex: Int) {
        if (node.isCollision()) {
            val index = node.buffer.indexOf(element)
            assert(index != -1)
            path[pathIndex].reset(node.buffer, index)
            pathLastIndex = pathIndex
            return
        }

        val position = 1 shl ((hashCode shr (pathIndex * LOG_MAX_BRANCHING_FACTOR)) and MAX_BRANCHING_FACTOR_MINUS_ONE)
        @UseExperimental(ExperimentalStdlibApi::class)
        val index = (node.bitmap and (position - 1)).countOneBits()

        path[pathIndex].reset(node.buffer, index)

        val cell = node.buffer[index]
        if (cell is TrieNode<*>) {
            resetPath(hashCode, cell, element, pathIndex + 1)
        } else {
//            assert(cell == element)
            pathLastIndex = pathIndex
        }
    }

    private fun checkNextWasInvoked() {
        if (!nextWasInvoked)
            throw IllegalStateException()
    }

    private fun checkForComodification() {
        if (builder.modCount != expectedModCount)
            throw ConcurrentModificationException()
    }
}