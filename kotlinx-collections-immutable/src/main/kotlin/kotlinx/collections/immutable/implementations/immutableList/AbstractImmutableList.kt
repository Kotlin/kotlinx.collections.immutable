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

abstract class AbstractImmutableList<E> : ImmutableList<E> {
    override fun add(element: E): ImmutableList<E> {
        return this.add(this.size, element)
    }

    override fun addAll(elements: Collection<E>): ImmutableList<E> {
        return this.addAll(this.size, elements)
    }

    override fun remove(element: E): ImmutableList<E> {
        val index = this.indexOf(element)
        if (index != -1) {
            return this.removeAt(index)
        }
        return this
    }

    override fun removeAll(elements: Collection<E>): ImmutableList<E> {
        return this.removeAll { elements.contains(it) }
    }

    override fun clear(): ImmutableList<E> {
        return persistentVectorOf()
    }

    override fun contains(element: E): Boolean {
        return this.indexOf(element) != -1
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { this.contains(it) }
    }

    override fun isEmpty(): Boolean {
        return this.size == 0
    }

    override fun iterator(): Iterator<E> {
        return this.listIterator()
    }

    override fun listIterator(): ListIterator<E> {
        return this.listIterator(0)
    }

    override fun toString(): String {
        return this.toList().toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is List<*>) {
            return false
        }
        return this.toList() == other
    }
}