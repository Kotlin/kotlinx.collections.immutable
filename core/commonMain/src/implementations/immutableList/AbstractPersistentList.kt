/*
 * Copyright 2016-2019 JetBrains s.r.o.
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

    override fun copyingAddAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return this
        return mutate { it.addAll(elements) }
    }

    override fun addingAll(index: Int, c: Collection<E>): PersistentList<E> {
        checkPositionIndex(index, size)
        if (c.isEmpty()) return this
        return mutate { it.addAll(index, c) }
    }

    override fun copyingRemove(element: E): PersistentList<E> {
        val index = this.indexOf(element)
        if (index != -1) {
            return this.removingAt(index)
        }
        return this
    }

    override fun copyingRemoveAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return this
        return copyingRemoveAll { elements.contains(it) }
    }

    override fun copyingRetainAll(elements: Collection<E>): PersistentList<E> {
        if (elements.isEmpty()) return persistentVectorOf()
        return copyingRemoveAll { !elements.contains(it) }
    }

    override fun copyingClear(): PersistentList<E> {
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
