/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedSet

import kotlinx.collections.immutable.PersistentOrderedSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class Links(val previous: Any?, val next: Any?) {
    /** Constructs Links for a new single element */
    constructor() : this(EndOfChain, EndOfChain)

    /** Constructs Links for a new last element */
    constructor(previous: Any?) : this(previous, EndOfChain)

    fun withNext(newNext: Any?) = Links(previous, newNext)
    fun withPrevious(newPrevious: Any?) = Links(newPrevious, next)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain
}

internal class PersistentOrderedSetImpl<E>(
    internal val firstElement: Any?,
    internal val lastElement: Any?,
    internal val hashMap: PersistentHashMap<E, Links>
) : AbstractSet<E>(), PersistentOrderedSet<E> {

    override val size: Int get() = hashMap.size

    override fun contains(element: E): Boolean = hashMap.containsKey(element)

    override fun adding(element: E): PersistentOrderedSetImpl<E> {
        if (hashMap.containsKey(element)) {
            return this
        }
        if (isEmpty()) {
            val newMap = hashMap.putting(element, Links())
            return PersistentOrderedSetImpl(element, element, newMap)
        }
        @Suppress("UNCHECKED_CAST")
        val lastElement = lastElement as E
        val lastLinks = hashMap[lastElement]!!
        assert { !lastLinks.hasNext }

        val newMap = hashMap
            .putting(lastElement, lastLinks.withNext(element))
            .putting(element, Links(previous = lastElement))
        return PersistentOrderedSetImpl(firstElement, element, newMap)
    }

    override fun addingAll(elements: Collection<E>): PersistentOrderedSetImpl<E> {
        if (elements.isEmpty()) return this
        return builder().apply { addAll(elements) }.build()
    }

    override fun removing(element: E): PersistentOrderedSetImpl<E> {
        val links = hashMap[element] ?: return this

        var newMap = hashMap.removing(element)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
            assert { previousLinks.next == element }
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.putting(links.previous as E, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
            assert { nextLinks.previous == element }
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.putting(links.next as E, nextLinks.withPrevious(links.previous))
        }
        val newFirstElement = if (!links.hasPrevious) links.next else firstElement
        val newLastElement = if (!links.hasNext) links.previous else lastElement
        return PersistentOrderedSetImpl(newFirstElement, newLastElement, newMap)
    }

    override fun removingAll(elements: Collection<E>): PersistentOrderedSetImpl<E> {
        if (elements.isEmpty()) return this
        return builder().apply { removeAll(elements) }.build()
    }

    override fun removingAll(predicate: (E) -> Boolean): PersistentOrderedSetImpl<E> {
        return builder().apply { removeAll(predicate) }.build()
    }

    override fun retainingAll(elements: Collection<E>): PersistentOrderedSetImpl<E> {
        if (elements.isEmpty()) return emptyOf()
        return builder().apply { retainAll(elements) }.build()
    }

    override fun cleared(): PersistentOrderedSetImpl<E> {
        return emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentOrderedSetIterator(firstElement, hashMap)
    }

    override fun builder(): PersistentOrderedSetBuilder<E> {
        return PersistentOrderedSetBuilder(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedSetImpl<*> -> hashMap.node.equalsWith(other.hashMap.node) { _, _ -> true }
            is PersistentOrderedSetBuilder<*> -> hashMap.node.equalsWith(other.hashMapBuilder.node) { _, _ -> true }
            else -> super.equals(other)
        }
    }

    /**
     * We provide [equals], so as a matter of style, we should also provide [hashCode].
     * However, the implementation from [AbstractSet] is enough.
     */
    override fun hashCode(): Int = super<AbstractSet>.hashCode()

    internal companion object {
        private val EMPTY = PersistentOrderedSetImpl<Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf())

        @Suppress("UNCHECKED_CAST")
        internal fun <E> emptyOf(): PersistentOrderedSetImpl<E> = EMPTY as PersistentOrderedSetImpl<E>
    }
}
