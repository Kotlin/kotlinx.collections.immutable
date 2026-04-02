/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate

internal class PersistentHashSet<E>(internal val node: TrieNode<E>,
                                    override val size: Int): AbstractSet<E>(), PersistentSet<E> {
    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    @Deprecated("Use adding() instead.", replaceWith = ReplaceWith("adding(element)"))
    override fun add(element: E): PersistentSet<E> {
        val newNode = node.add(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size + 1)
    }

    @Deprecated("Use addingAll() instead.", replaceWith = ReplaceWith("addingAll(elements)"))
    override fun addAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return this
        return this.mutate { it.addAll(elements) }
    }

    @Deprecated("Use removing() instead.", replaceWith = ReplaceWith("removing(element)"))
    override fun remove(element: E): PersistentSet<E> {
        val newNode = node.remove(element.hashCode(), element, 0)
        if (node === newNode) { return this }
        return PersistentHashSet(newNode, size - 1)
    }

    @Deprecated("Use removingAll() instead.", replaceWith = ReplaceWith("removingAll(elements)"))
    override fun removeAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return this
        return mutate { it.removeAll(elements) }
    }

    @Deprecated("Use removingAll() instead.", replaceWith = ReplaceWith("removingAll(predicate)"))
    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    @Deprecated("Use retainingAll() instead.", replaceWith = ReplaceWith("retainingAll(elements)"))
    override fun retainAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return PersistentHashSet.emptyOf<E>()
        return mutate { it.retainAll(elements) }
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

    @Deprecated("Use cleared() instead.", replaceWith = ReplaceWith("cleared()"))
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
