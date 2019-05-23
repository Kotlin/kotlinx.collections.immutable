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

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate

internal class PersistentHashSet<E>(internal val node: TrieNode<E>,
                                    override val size: Int): AbstractSet<E>(), PersistentSet<E> {
    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    override fun add(element: E): PersistentSet<E> {
        val newNode = node.add(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size + 1)
    }

    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        return this.mutate { it.addAll(elements) }
    }

    override fun remove(element: E): PersistentSet<E> {
        val newNode = node.remove(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        if (newNode == null) { return PersistentHashSet.emptyOf() }
        return PersistentHashSet(newNode, size - 1)
    }

    override fun removeAll(elements: Collection<E>): PersistentSet<E> {
        return mutate { it.removeAll(elements) }
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun clear(): PersistentSet<E> {
        return PersistentHashSet.emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentHashSetIterator(node)
    }

    override fun builder(): PersistentSet.Builder<E> {
        return PersistentHashSetBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentHashSet(TrieNode.EMPTY, 0)
        internal fun <E> emptyOf(): PersistentSet<E> = PersistentHashSet.EMPTY
    }
}
