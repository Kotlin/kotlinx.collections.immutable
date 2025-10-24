/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.mutate

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

internal class PersistentOrderedSet<E>(
        internal val firstElement: Any?,
        internal val lastElement: Any?,
        internal val hashMap: PersistentHashMap<E, Links>
) : AbstractSet<E>(), PersistentSet<E> {

    override val size: Int get() = hashMap.size

    override fun contains(element: E): Boolean = hashMap.containsKey(element)

    override fun adding(element: E): PersistentSet<E> {
        if (hashMap.containsKey(element)) {
            return this
        }
        if (isEmpty()) {
            val newMap = hashMap.put(element, Links())
            return PersistentOrderedSet(element, element, newMap)
        }
        @Suppress("UNCHECKED_CAST")
        val lastElement = lastElement as E
        val lastLinks = hashMap[lastElement]!!
//        assert(!lastLinks.hasNext)

        val newMap = hashMap
                .put(lastElement, lastLinks.withNext(element))
                .put(element, Links(previous = lastElement))
        return PersistentOrderedSet(firstElement, element, newMap)
    }

    override fun addingAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return this
        return this.mutate { it.addAll(elements) }
    }

    override fun removing(element: E): PersistentSet<E> {
        val links = hashMap[element] ?: return this

        var newMap = hashMap.remove(element)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == element)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.previous as E, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == element)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.next as E, nextLinks.withPrevious(links.previous))
        }
        val newFirstElement = if (!links.hasPrevious) links.next else firstElement
        val newLastElement = if (!links.hasNext) links.previous else lastElement
        return PersistentOrderedSet(newFirstElement, newLastElement, newMap)
    }

    override fun removingAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return this
        return mutate { it.removeAll(elements) }
    }

    override fun removingAll(predicate: (E) -> Boolean): PersistentSet<E> {
        return mutate { it.removeAll(predicate) }
    }

    override fun retainingAll(elements: Collection<E>): PersistentSet<E> {
        if (elements.isEmpty()) return PersistentOrderedSet.emptyOf<E>()
        return mutate { it.retainAll(elements) }
    }

    override fun cleared(): PersistentSet<E> {
        return PersistentOrderedSet.emptyOf()
    }

    override fun iterator(): Iterator<E> {
        return PersistentOrderedSetIterator(firstElement, hashMap)
    }

    override fun builder(): PersistentSet.Builder<E> {
        return PersistentOrderedSetBuilder(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedSet<*> -> {
                hashMap.node.equalsWith(other.hashMap.node) { _, _ -> true }
            }
            is PersistentOrderedSetBuilder<*> -> {
                hashMap.node.equalsWith(other.hashMapBuilder.node) { _, _ -> true }
            }
            else -> super.equals(other)
        }
    }

    /**
     * We provide [equals], so as a matter of style, we should also provide [hashCode].
     * However, the implementation from [AbstractSet] is enough.
     */
    override fun hashCode(): Int = super<AbstractSet>.hashCode()

    internal companion object {
        private val EMPTY = PersistentOrderedSet<Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf())
        internal fun <E> emptyOf(): PersistentSet<E> = EMPTY
    }
}
