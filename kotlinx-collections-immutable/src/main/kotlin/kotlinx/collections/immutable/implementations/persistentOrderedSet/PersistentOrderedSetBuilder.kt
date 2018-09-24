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

internal class PersistentOrderedSetBuilder<E>(private var set: PersistentOrderedSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var firstElement = set.firstElement
    private var lastElement = set.lastElement
    internal val mapBuilder = set.map.builder()

    override val size: Int
        get() = mapBuilder.size

    override fun build(): PersistentSet<E> {
        val newMap = mapBuilder.build()
        set = if (newMap === set.map) {
            assert(firstElement === set.firstElement)
            assert(lastElement === set.lastElement)
            set
        } else {
            PersistentOrderedSet(firstElement, lastElement, newMap)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        return mapBuilder.containsKey(element)
    }

    override fun add(element: E): Boolean {
        if (mapBuilder.containsKey(element)) {
            return false
        }
        if (lastElement == null) {  //  isEmpty
            firstElement = element
            lastElement = element
            mapBuilder[element] = Links<E>(null, null)
            return true
        }
        val oldLinks = mapBuilder[lastElement!!]!!
        assert(oldLinks.next == null)
        val newLinks = Links(oldLinks.previous, element)

        mapBuilder[lastElement!!] = newLinks
        mapBuilder[element] = Links(lastElement, null)
        lastElement = element
        return true
    }

    override fun remove(element: E): Boolean {
        val links = mapBuilder.remove(element) ?: return false

        if (links.previous != null) {
            val previousLinks = mapBuilder[links.previous]!!
            assert(previousLinks.next == element)
            mapBuilder[links.previous] = Links(previousLinks.previous, links.next)
        }
        if (links.next != null) {
            val nextLinks = mapBuilder[links.next]!!
            assert(nextLinks.previous == element)
            mapBuilder[links.next] = Links(links.previous, nextLinks.next)
        }
        firstElement = if (element == firstElement) links.next else firstElement
        lastElement = if (element == lastElement) links.previous else lastElement
        return true
    }

    override fun clear() {
        mapBuilder.clear()
        firstElement = null
        lastElement = null
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentOrderedSetMutableIterator(this)
    }
}