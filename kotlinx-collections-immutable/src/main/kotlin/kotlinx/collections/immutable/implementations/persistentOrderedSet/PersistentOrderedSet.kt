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
import kotlinx.collections.immutable.mutate

internal class Links<out E>(val previous: E?, val next: E?)

internal class PersistentOrderedSet<E>(internal val firstElement: E?,
                                       internal val lastElement: E?,
                                       internal val map: PersistentHashMap<E, Links<E>>): AbstractSet<E>(), PersistentSet<E> {

    override val size: Int
        get() = map.size

    override fun contains(element: E): Boolean {
        return map.containsKey(element)
    }

    override fun add(element: E): PersistentSet<E> {
        if (map.containsKey(element)) {
            return this
        }
        if (lastElement == null) {  //  isEmpty
            val newMap = map.put(element, Links<E>(null, null))
            return PersistentOrderedSet(element, element, newMap)
        }
        val oldLinks = map[lastElement]!!
        assert(oldLinks.next == null)
        val newLinks = Links(oldLinks.previous, element)
        val newMap = map.put(lastElement, newLinks).put(element, Links(lastElement, null))
        return PersistentOrderedSet(firstElement, element, newMap)
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): PersistentSet<E> {
        val links = map[element] ?: return this

        var newMap = map.remove(element)
        if (links.previous != null) {
            val previousLinks = newMap[links.previous]!!
            assert(previousLinks.next == element)
            newMap = newMap.put(links.previous, Links(previousLinks.previous, links.next))
        }
        if (links.next != null) {
            val nextLinks = newMap[links.next]!!
            assert(nextLinks.previous == element)
            newMap = newMap.put(links.next, Links(links.previous, nextLinks.next))
        }
        val newFirstElement = if (element == firstElement) links.next else firstElement
        val newLastElement = if (element == lastElement) links.previous else lastElement
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
