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

package kotlinx.collections.immutable.implementations.persistentOrderedSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.EndOfChain
import kotlinx.collections.immutable.mutate

internal class Links(val previous: Any?, val next: Any?) {
    /** Constructs Links for a new single element */
    constructor() : this(EndOfChain, EndOfChain)
    /** Constructs Links for a new last element */
    constructor(previous: Any?) : this(previous, EndOfChain)

    fun withNext(newNext: Any?) = Links(previous, newNext)
    fun withPrevious(newPrevious: Any?) = Links(newPrevious, next)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain
}

internal class PersistentOrderedSet<E>(
        internal val firstElement: Any?,
        internal val lastElement: Any?,
        internal val hashMap: PersistentHashMap<E, Links>
) : AbstractSet<E>(), PersistentSet<E> {

    override val size: Int get() = hashMap.size

    override fun contains(element: E): Boolean = hashMap.containsKey(element)

    override fun add(element: E): PersistentSet<E> {
        if (hashMap.containsKey(element)) {
            return this
        }
        if (isEmpty()) {
            val newMap = hashMap.put(element, Links())
            return PersistentOrderedSet(element, element, newMap)
        }
        @Suppress("UNCHECKED_CAST")
        val lastElement = lastElement as E
        val lastLinks = hashMap[lastElement]!!
//        assert(!lastLinks.hasNext)

        val newMap = hashMap
                .put(lastElement, lastLinks.withNext(element))
                .put(element, Links(previous = lastElement))
        return PersistentOrderedSet(firstElement, element, newMap)
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): PersistentSet<E> {
        val links = hashMap[element] ?: return this

        var newMap = hashMap.remove(element)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == element)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.previous as E, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == element)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.next as E, nextLinks.withPrevious(links.previous))
        }
        val newFirstElement = if (!links.hasPrevious) links.next else firstElement
        val newLastElement = if (!links.hasNext) links.previous else lastElement
        return PersistentOrderedSet(newFirstElement, newLastElement, newMap)
    }

    override fun removeAll(elements: Collection<E>): PersistentSet<E> {
        return mutate { it.removeAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun clear(): PersistentSet<E> {
        return PersistentOrderedSet.emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentOrderedSetIterator(firstElement, hashMap)
    }

    override fun builder(): PersistentSet.Builder<E> {
        return PersistentOrderedSetBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentOrderedSet<Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf<Nothing, Links>())
        internal fun <E> emptyOf(): PersistentSet<E> = EMPTY
    }
}
