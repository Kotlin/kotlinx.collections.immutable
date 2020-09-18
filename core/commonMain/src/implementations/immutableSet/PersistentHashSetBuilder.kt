/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.internal.DeltaCounter
import kotlinx.collections.immutable.internal.MutabilityOwnership

internal class PersistentHashSetBuilder<E>(private var set: PersistentHashSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var ownership = MutabilityOwnership()
        private set
    internal var node = set.node
        private set
    internal var modCount = 0
        private set

    // Size change implies structural changes.
    override var size = set.size
        set(value) {
            field = value
            modCount++
        }

    override fun build(): PersistentSet<E> {
        set = if (node === set.node) {
            set
        } else {
            ownership = MutabilityOwnership()
            PersistentHashSet(node, size)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        return node.contains(element.hashCode(), element, 0)
    }

    override fun add(element: E): Boolean {
        val size = this.size
        node = node.mutableAdd(element.hashCode(), element, 0, this)
        return size != this.size
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val set = when {
            elements is PersistentHashSet -> elements
            elements is PersistentHashSetBuilder && elements.node === elements.set.node -> elements.set
            else -> null
        }
        if (set !== null) {
            val deltaCounter = DeltaCounter()
            val oldSize = this.size
            node = node.mutableAddAll(set.node, 0, deltaCounter, this)
            val newSize = oldSize + elements.size - deltaCounter.count
            if (oldSize != newSize) {
                this.size = newSize
            }
            return oldSize != this.size
        }
        return super.addAll(elements)
    }

    override fun remove(element: E): Boolean {
        val size = this.size
        @Suppress("UNCHECKED_CAST")
        node = node.mutableRemove(element.hashCode(), element, 0, this)
        return size != this.size
    }

    override fun clear() {
        @Suppress("UNCHECKED_CAST")
        node = TrieNode.EMPTY as TrieNode<E>
        size = 0
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentHashSetMutableIterator(this)
    }
}