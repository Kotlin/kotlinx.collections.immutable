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
import kotlinx.collections.immutable.implementations.persistentOrderedMap.EndOfLink
import kotlinx.collections.immutable.mutate

internal class Links(val previous: Any?, val next: Any?) {
    constructor() : this(EndOfLink, EndOfLink)

    fun withNext(newNext: Any?) = Links(previous, newNext)
    fun withPrevious(newPrevious: Any?) = Links(newPrevious, next)

    fun putNextLink(previous: Any?): Links {
//        assert(next === EndOfLink)
        return Links(previous, next)
    }
}

internal class PersistentOrderedSet<E>(internal val firstElement: E?,
                                       internal val lastElement: E?,
                                       internal val map: PersistentHashMap<E, Links>): AbstractSet<E>(), PersistentSet<E> {

    override val size: Int
        get() = map.size

    override fun contains(element: E): Boolean {
        return map.containsKey(element)
    }

    override fun add(element: E): PersistentSet<E> {
        if (map.containsKey(element)) {
            return this
        }
        if (isEmpty()) {  //  isEmpty
            val newMap = map.put(element, Links())
            return PersistentOrderedSet(element, element, newMap)
        }
        val lastLinks = map[lastElement]!!
//        assert(lastLinks.next === EndOfLink)

        val newMap = map
                .put(lastElement as E, lastLinks.withNext(element))
                .put(element, lastLinks.putNextLink(lastElement))
        return PersistentOrderedSet(firstElement, element, newMap)
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): PersistentSet<E> {
        val links = map[element] ?: return this

        var newMap = map.remove(element)
        if (links.previous !== EndOfLink) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == element)
            newMap = newMap.put(links.previous as E, previousLinks.withNext(links.next))
        }
        if (links.next !== EndOfLink) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == element)
            newMap = newMap.put(links.next as E, nextLinks.withPrevious(links.previous))
        }
        val newFirstElement = if (links.previous === EndOfLink) links.next as? E else firstElement
        val newLastElement = if (links.next === EndOfLink) links.previous as? E else lastElement
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
        return PersistentOrderedSetIterator(firstElement, map)
    }

    override fun builder(): PersistentSet.Builder<E> {
        return PersistentOrderedSetBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentOrderedSet(null, null, PersistentHashMap.emptyOf())
        internal fun <E> emptyOf(): PersistentSet<E> = EMPTY
    }
}
