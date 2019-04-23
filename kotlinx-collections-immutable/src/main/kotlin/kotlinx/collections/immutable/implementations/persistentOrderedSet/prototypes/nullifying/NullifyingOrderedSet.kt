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

package kotlinx.collections.immutable.implementations.persistentOrderedSet.prototypes.nullifying

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableList.persistentVectorOf
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.mutate

private object NullMarker

internal class NullifyingOrderedSet<E>(
        private val hashMap: PersistentHashMap<E, Int>,
        private val list: PersistentList<E>
) : PersistentSet<E>, AbstractSet<E>() {

    override val size: Int get() = hashMap.size

    override fun contains(element: E): Boolean = hashMap.contains(element)

    override fun add(element: E): PersistentSet<E> {
        if (hashMap.containsKey(element))
            return this

        return NullifyingOrderedSet(
                hashMap.put(element, list.size),
                list.add(element)
        )
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun reindex(list: PersistentList<E>): NullifyingOrderedSet<E> {
        val newList = list.removeAll { it === NullMarker }
        val newHashMap = PersistentHashMap.emptyOf<E, Int>().mutate {
            newList.forEachIndexed { index, e -> it[e] = index }
        }

        return NullifyingOrderedSet(newHashMap as PersistentHashMap<E, Int>, newList)
    }

    override fun remove(element: E): PersistentSet<E> {
        val index = hashMap[element] ?: return this

        val magicSize = 32
        val magicRation = 2
        if (list.size > magicSize && list.size > hashMap.size * magicRation) {
            return reindex(list.set(index, NullMarker as E))
        }

        @Suppress("UNCHECKED_CAST")
        return NullifyingOrderedSet(
                hashMap.remove(element),
                list.set(index, NullMarker as E)
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
        return object : Iterator<E> {
            val index = 0
            val listIterator = list.iterator()

            override fun hasNext(): Boolean {
                return index < this@NullifyingOrderedSet.size
            }

            override fun next(): E {
                require(hasNext())

                var result: Any? = NullMarker
                while (result === NullMarker) { result = listIterator.next() }

                @Suppress("UNCHECKED_CAST")
                return result as E
            }
        }
    }

    internal companion object {
        private val EMPTY = NullifyingOrderedSet<Nothing>(PersistentHashMap.emptyOf(), persistentVectorOf())
        internal fun <E> emptyOf(): PersistentSet<E> = EMPTY
    }
}