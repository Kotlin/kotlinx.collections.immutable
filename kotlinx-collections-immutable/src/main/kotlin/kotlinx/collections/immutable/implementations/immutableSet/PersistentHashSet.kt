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

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.mutate

internal class PersistentHashSet<E>(private val node: TrieNode<E>,
                                    override val size: Int): ImmutableSet<E> {
    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun contains(element: E): Boolean {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        return node.contains(hashCode, element, 0)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { contains(it) }
    }

    override fun add(element: E): ImmutableSet<E> {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        val newNode = node.add(hashCode, element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size + 1)
    }

    override fun addAll(elements: Collection<E>): ImmutableSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): ImmutableSet<E> {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        val newNode = node.remove(hashCode, element, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return persistentHashSetOf() }
        return PersistentHashSet(newNode, size - 1)
    }

    override fun removeAll(elements: Collection<E>): ImmutableSet<E> {
        return mutate { it.removeAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): ImmutableSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun clear(): ImmutableSet<E> {
        return persistentHashSetOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentHashSetIterator(node)
    }

    override fun builder(): ImmutableSet.Builder<E> {
        return PersistentHashSetBuilder(node, size)
    }

    internal companion object {
        internal val EMPTY = PersistentHashSet(TrieNode.EMPTY, 0)
    }
}

fun <E> persistentHashSetOf(): ImmutableSet<E> {
    return PersistentHashSet.EMPTY
}