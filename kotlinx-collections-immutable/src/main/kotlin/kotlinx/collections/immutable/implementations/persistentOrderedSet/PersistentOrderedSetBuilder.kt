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
import kotlinx.collections.immutable.implementations.persistentOrderedMap.EndOfLink

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
        if (isEmpty()) {
            firstElement = element
            lastElement = element
            mapBuilder[element] = Links()
            return true
        }

        val lastLinks = mapBuilder[lastElement]!!
//        assert(lastLinks.next === EndOfLink)
        mapBuilder[lastElement as E] = lastLinks.withNext(element)
        mapBuilder[element] = lastLinks.putNextLink(lastElement)
        lastElement = element

        return true
    }

    override fun remove(element: E): Boolean {
        val links = mapBuilder.remove(element) ?: return false

        if (links.previous !== EndOfLink) {
            val previousLinks = mapBuilder[links.previous]!!
//            assert(previousLinks.next == element)
            mapBuilder[links.previous as E] = previousLinks.withNext(links.next)
        } else {
            firstElement = links.next
        }
        if (links.next !== EndOfLink) {
            val nextLinks = mapBuilder[links.next]!!
//            assert(nextLinks.previous == element)
            mapBuilder[links.next as E] = nextLinks.withPrevious(links.previous)
        } else {
            lastElement = links.previous
        }

        return true
    }

    override fun clear() {
        mapBuilder.clear()
        firstElement = EndOfLink
        lastElement = EndOfLink
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentOrderedSetMutableIterator(this)
    }
}