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
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class PersistentOrderedSetBuilder<E>(private var set: PersistentOrderedSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var firstElement = set.firstElement
    private var lastElement = set.lastElement
    internal val hashMapBuilder = set.hashMap.builder()

    override val size: Int
        get() = hashMapBuilder.size

    override fun build(): PersistentSet<E> {
        val newMap = hashMapBuilder.build()
        set = if (newMap === set.hashMap) {
            assert(firstElement === set.firstElement)
            assert(lastElement === set.lastElement)
            set
        } else {
            PersistentOrderedSet(firstElement, lastElement, newMap)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        return hashMapBuilder.containsKey(element)
    }

    override fun add(element: E): Boolean {
        if (hashMapBuilder.containsKey(element)) {
            return false
        }
        if (isEmpty()) {
            firstElement = element
            lastElement = element
            hashMapBuilder[element] = Links()
            return true
        }

        val lastLinks = hashMapBuilder[lastElement]!!
//        assert(!lastLinks.hasNext)
        @Suppress("UNCHECKED_CAST")
        hashMapBuilder[lastElement as E] = lastLinks.withNext(element)
        hashMapBuilder[element] = Links(previous = lastElement)
        lastElement = element

        return true
    }

    override fun remove(element: E): Boolean {
        val links = hashMapBuilder.remove(element) ?: return false

        if (links.hasPrevious) {
            val previousLinks = hashMapBuilder[links.previous]!!
//            assert(previousLinks.next == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.previous as E] = previousLinks.withNext(links.next)
        } else {
            firstElement = links.next
        }
        if (links.hasNext) {
            val nextLinks = hashMapBuilder[links.next]!!
//            assert(nextLinks.previous == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.next as E] = nextLinks.withPrevious(links.previous)
        } else {
            lastElement = links.previous
        }

        return true
    }

    override fun clear() {
        hashMapBuilder.clear()
        firstElement = EndOfChain
        lastElement = EndOfChain
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentOrderedSetMutableIterator(this)
    }
}