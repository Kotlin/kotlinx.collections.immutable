/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.PersistentUnorderedSet

internal class PersistentHashSet<E>(
    internal val node: TrieNode<E>,
    override val size: Int
) : AbstractSet<E>(), PersistentUnorderedSet<E> {
    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    override fun adding(element: E): PersistentHashSet<E> {
        val newNode = node.add(element.hashCode(), element, 0)
        if (node === newNode) return this
        return PersistentHashSet(newNode, size + 1)
    }

    override fun addingAll(elements: Collection<E>): PersistentHashSet<E> {
        if (elements.isEmpty()) return this
        return builder().apply { addAll(elements) }.build()
    }

    override fun removing(element: E): PersistentHashSet<E> {
        val newNode = node.remove(element.hashCode(), element, 0)
        if (node === newNode) return this
        return PersistentHashSet(newNode, size - 1)
    }

    override fun removingAll(elements: Collection<E>): PersistentHashSet<E> {
        if (elements.isEmpty()) return this
        return builder().apply { removeAll(elements) }.build()
    }

    override fun removingAll(predicate: (E) -> Boolean): PersistentHashSet<E> {
        return builder().apply { removeAll(predicate) }.build()
    }

    override fun retainingAll(elements: Collection<E>): PersistentHashSet<E> {
        if (elements.isEmpty()) return emptyOf()
        return builder().apply { retainAll(elements) }.build()
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements is PersistentHashSet) {
            return node.containsAll(elements.node, 0)
        }
        if (elements is PersistentHashSetBuilder) {
            return node.containsAll(elements.node, 0)
        }
        return super.containsAll(elements)
    }

    override fun cleared(): PersistentHashSet<E> {
        return emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentHashSetIterator(node)
    }

    override fun builder(): PersistentHashSetBuilder<E> {
        return PersistentHashSetBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentHashSet(TrieNode.EMPTY, 0)

        @Suppress("UNCHECKED_CAST")
        internal fun <E> emptyOf(): PersistentHashSet<E> = EMPTY as PersistentHashSet<E>
    }
}
