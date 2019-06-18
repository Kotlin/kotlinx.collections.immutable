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

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import java.util.Random

private interface ReidentificationStrategy {
    fun shouldReidentify(size: Int, id: Int): Boolean
}

private val ReidentifyOnOverflow = object : ReidentificationStrategy {
    override fun shouldReidentify(size: Int, id: Int): Boolean = id == Int.MAX_VALUE
}

private val ReidentifyAtRandom = object : ReidentificationStrategy {
    val random = Random()
    val bar = 1 shl 30

    override fun shouldReidentify(size: Int, id: Int): Boolean {
        if (id < bar)
            return false

        val idsLeft = Int.MAX_VALUE - id + 1
        return random.nextInt() % idsLeft == 0
    }
}

internal class UniqueIdOrderedSet<E>(
        private val elementToId: PersistentHashMap<E, Int>,
        private val idToElementRoot: OrderedTrieNode<E>,
        private val idToElementTail: OrderedTrieNode<E>,
        private val uniqueId: Int,
        private val rootShift: Int
) : PersistentSet<E>, AbstractSet<E>() {

    override val size: Int get() = elementToId.size

    override fun contains(element: E): Boolean = elementToId.contains(element)

    private val strategy: ReidentificationStrategy
//        get() = ReidentifyOnOverflow
        get() = ReidentifyAtRandom

    private fun reidentifyElements() : UniqueIdOrderedSet<E> {
        val elementToIdBuilder = PersistentHashMap.emptyOf<E, Int>().builder()
        var newRoot = OrderedTrieNode.emptyOf<E>()
        var newTailBuffer = arrayOfNulls<Any?>(MAX_BRANCHING_FACTOR)
        var newTailSize = 0
        var newRootShift = 0

        this.forEachIndexed { id, element ->
            elementToIdBuilder[element] = id

            if (newTailSize == MAX_BRANCHING_FACTOR) {
                if (shouldIncreaseRootHeight(id, newRootShift)) {
                    newRoot = OrderedTrieNode(newRoot)
                    newRootShift += LOG_MAX_BRANCHING_FACTOR
                }
                newRoot = newRoot.pushTail(id - 1, newRootShift, OrderedTrieNode(-1, newTailBuffer))
                newTailBuffer = arrayOfNulls<Any?>(MAX_BRANCHING_FACTOR)
                newTailSize = 0
            }

            newTailBuffer[newTailSize++] = element
        }

        return UniqueIdOrderedSet(
                elementToIdBuilder.build(),
                newRoot,
                OrderedTrieNode((1 shl newTailSize) - 1, newTailBuffer.copyOf(newTailSize)),
                size,
                newRootShift
        )
    }

    private fun shouldPushTailToRoot(id: Int): Boolean {
        return id and MAX_BRANCHING_FACTOR_MINUS_ONE == 0 && id != Int.MIN_VALUE
    }

    private fun shouldPushTailToRoot(): Boolean  = shouldPushTailToRoot(uniqueId)

    private fun shouldIncreaseRootHeight(id: Int, rootShift: Int): Boolean {
        return (id and Int.MAX_VALUE) shr LOG_MAX_BRANCHING_FACTOR >= 1 shl rootShift
    }

    private fun shouldIncreaseRootHeight(): Boolean = shouldIncreaseRootHeight(uniqueId, rootShift)

    override fun add(element: E): PersistentSet<E> {
        if (elementToId.containsKey(element))
            return this

        if (strategy.shouldReidentify(size, uniqueId)) {
            return reidentifyElements().add(element)
        }

        if (shouldPushTailToRoot()) {
            // TODO: empty tail should not be pushed

            val newTail = OrderedTrieNode.emptyOf<E>().add(uniqueId, element)

            if (shouldIncreaseRootHeight()) {
                val newRoot = (if (rootShift == 0) idToElementRoot else OrderedTrieNode(idToElementRoot))
                        .pushTail(uniqueId - 1, rootShift + LOG_MAX_BRANCHING_FACTOR, idToElementTail)

                return UniqueIdOrderedSet(
                        elementToId.put(element, uniqueId),
                        newRoot,
                        newTail,
                        uniqueId + 1,
                        rootShift + LOG_MAX_BRANCHING_FACTOR
                )
            }

            val newRoot = idToElementRoot.pushTail(uniqueId - 1, rootShift, idToElementTail)

            return UniqueIdOrderedSet(
                    elementToId.put(element, uniqueId),
                    newRoot,
                    newTail,
                    uniqueId + 1,
                    rootShift
            )
        }

        return UniqueIdOrderedSet(
                elementToId.put(element, uniqueId),
                idToElementRoot,
                idToElementTail.add(uniqueId, element),
                uniqueId + 1,
                rootShift
        )
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun isElementInTail(elementId: Int): Boolean {
        return (elementId shr LOG_MAX_BRANCHING_FACTOR) == ((uniqueId - 1) shr LOG_MAX_BRANCHING_FACTOR)
    }

    override fun remove(element: E): PersistentSet<E> {
        val idToRemove = elementToId[element] ?: return this

        if (size == 1) return emptyOf()

        if (isElementInTail(idToRemove)) {
            return UniqueIdOrderedSet(
                    elementToId.remove(element),
                    idToElementRoot,
                    idToElementTail.remove(idToRemove, element, 0) ?: OrderedTrieNode.emptyOf(),
                    uniqueId,
                    rootShift
            )
        }

        return UniqueIdOrderedSet(
                elementToId.remove(element),
                idToElementRoot.remove(idToRemove, element, rootShift) ?: OrderedTrieNode.emptyOf(),
                idToElementTail,
                uniqueId,
                rootShift
        )
    }

    override fun removeAll(elements: Collection<E>): PersistentSet<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear(): PersistentSet<E> {
        return emptyOf()
    }

    override fun builder(): PersistentSet.Builder<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<E> {
        var negativeIdRoot: OrderedTrieNode<E>? = null
        var positiveIdRoot: OrderedTrieNode<E>? = null
        if (rootShift == MAX_SHIFT) {
            assert(uniqueId >= 0)

            if (idToElementRoot.bitmap and 1 != 0) {
                positiveIdRoot = idToElementRoot.buffer[0] as OrderedTrieNode<E>
            }
            if (idToElementRoot.bitmap and 2 != 0) {
                negativeIdRoot = idToElementRoot.buffer[1] as OrderedTrieNode<E>
            }
        } else {
            negativeIdRoot = idToElementRoot
        }

        return object : Iterator<E> {

            private val negativeIdRootIterator = negativeIdRoot?.let { RootOrderedTrieIterator(it) }
            private val positiveIdRootIterator = positiveIdRoot?.let { RootOrderedTrieIterator(it) }
            private val tailIterator: Iterator<E> = (idToElementTail.buffer as Array<E>).iterator()

            override fun hasNext(): Boolean {
                return negativeIdRootIterator?.hasNext() == true
                        || positiveIdRootIterator?.hasNext() == true
                        || tailIterator.hasNext()
            }

            override fun next(): E {
                if (!hasNext())
                    throw NoSuchElementException()

                return when {
                    negativeIdRootIterator?.hasNext() == true -> negativeIdRootIterator.next()
                    positiveIdRootIterator?.hasNext() == true -> positiveIdRootIterator.next()
                    else -> tailIterator.next()
                }
            }
        }
    }

    internal companion object {
        private val EMPTY = UniqueIdOrderedSet(PersistentHashMap.emptyOf(), OrderedTrieNode.EMPTY, OrderedTrieNode.EMPTY, Int.MIN_VALUE, 0)
        internal fun <E> emptyOf(): PersistentSet<E> = EMPTY
    }
}

public fun <E> uniqueIdOrderedSetOf(): PersistentSet<E> {
    return UniqueIdOrderedSet.emptyOf()
}