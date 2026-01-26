/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableList

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.internal.ListImplementation.checkPositionIndex

public abstract class AbstractPersistentList<E> : PersistentList<E>, AbstractList<E>() {
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
        return super<PersistentList>.subList(fromIndex, toIndex)
    }

    @Deprecated("Use addingAll() instead.", replaceWith = ReplaceWith("addingAll(elements)"))
    override fun addAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return this
        return mutate { it.addAll(elements) }
    }

    @Deprecated("Use insertingAllAt(index, c) instead.", replaceWith = ReplaceWith("insertingAllAt(index, c)"))
    override fun addAll(index: Int, c: Collection<E>): PersistentList<E> {
        checkPositionIndex(index, size)
        if (c.isEmpty()) return this
        return mutate { it.addAll(index, c) }
    }

    @Deprecated("Use removing() instead.", replaceWith = ReplaceWith("removing(element)"))
    override fun remove(element: E): PersistentList<E> {
        val index = this.indexOf(element)
        if (index != -1) {
            return this.removingAt(index)
        }
        return this
    }

    @Deprecated("Use removingAll() instead.", replaceWith = ReplaceWith("removingAll(elements)"))
    override fun removeAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return this
        return removingAll { elements.contains(it) }
    }

    @Deprecated("Use retainingAll() instead.", replaceWith = ReplaceWith("retainingAll(elements)"))
    override fun retainAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return persistentVectorOf()
        return removingAll { !elements.contains(it) }
    }

    @Deprecated("Use cleared() instead.", replaceWith = ReplaceWith("cleared()"))
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
