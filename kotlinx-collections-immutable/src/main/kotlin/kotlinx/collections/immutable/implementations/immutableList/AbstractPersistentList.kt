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

package kotlinx.collections.immutable.implementations.immutableList

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate

abstract class AbstractPersistentList<E> : PersistentList<E>, AbstractList<E>() {
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
        return super<PersistentList>.subList(fromIndex, toIndex)
    }

    override fun addAll(elements: Collection<E>): PersistentList<E> {
        return mutate { it.addAll(elements) }
    }

    override fun addAll(index: Int, c: Collection<E>): PersistentList<E> {
        return mutate { it.addAll(index, c) }
    }

    override fun remove(element: E): PersistentList<E> {
        val index = this.indexOf(element)
        if (index != -1) {
            return this.removeAt(index)
        }
        return this
    }

    override fun removeAll(elements: Collection<E>): PersistentList<E> {
        return mutate { it.removeAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun clear(): PersistentList<E> {
        return persistentVectorOf()
    }

    override fun contains(element: E): Boolean {
        return this.indexOf(element) != -1
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { this.contains(it) }
    }

    override fun iterator(): Iterator<E> {
        return this.listIterator()
    }

    override fun listIterator(): ListIterator<E> {
        return this.listIterator(0)
    }
}