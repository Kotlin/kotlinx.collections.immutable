/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.persistentOrderedSet

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.internal.EndOfChain
import kotlinx.collections.immutable.internal.assert

internal class PersistentOrderedSetBuilder<E>(set: PersistentOrderedSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    private var builtSet: PersistentOrderedSet<E>? = set
    internal var firstElement = set.firstElement
    private var lastElement = set.lastElement
    internal val hashMapBuilder = set.hashMap.builder()

    override val size: Int
        get() = hashMapBuilder.size

    override fun build(): PersistentSet<E> {
        return builtSet?.also { set ->
            assert(hashMapBuilder.builtMap != null)
            assert(firstElement === set.firstElement)
            assert(lastElement === set.lastElement)
        } ?: run {
            assert(hashMapBuilder.builtMap == null)
            val newMap = hashMapBuilder.build()
            val newSet = PersistentOrderedSet(firstElement, lastElement, newMap)
            builtSet = newSet
            newSet
        }
    }

    override fun contains(element: E): Boolean {
        return hashMapBuilder.containsKey(element)
    }

    override fun add(element: E): Boolean {
        if (hashMapBuilder.containsKey(element)) {
            return false
        }
        builtSet = null
        if (isEmpty()) {
            firstElement = element
            lastElement = element
            hashMapBuilder[element] = Links()
            return true
        }

        val lastLinks = hashMapBuilder[lastElement]!!
//        assert(!lastLinks.hasNext)
        @Suppress("UNCHECKED_CAST")
        hashMapBuilder[lastElement as E] = lastLinks.withNext(element)
        hashMapBuilder[element] = Links(previous = lastElement)
        lastElement = element

        return true
    }

    override fun remove(element: E): Boolean {
        val links = hashMapBuilder.remove(element) ?: return false
        builtSet = null
        if (links.hasPrevious) {
            val previousLinks = hashMapBuilder[links.previous]!!
//            assert(previousLinks.next == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.previous as E] = previousLinks.withNext(links.next)
        } else {
            firstElement = links.next
        }
        if (links.hasNext) {
            val nextLinks = hashMapBuilder[links.next]!!
//            assert(nextLinks.previous == element)
            @Suppress("UNCHECKED_CAST")
            hashMapBuilder[links.next as E] = nextLinks.withPrevious(links.previous)
        } else {
            lastElement = links.previous
        }

        return true
    }

    override fun clear() {
        if (hashMapBuilder.isNotEmpty()) {
            builtSet = null
        }
        hashMapBuilder.clear()
        firstElement = EndOfChain
        lastElement = EndOfChain
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentOrderedSetMutableIterator(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        if (size != other.size) return false

        return when (other) {
            is PersistentOrderedSet<*> -> {
                hashMapBuilder.node.equalsWith(other.hashMap.node) { _, _ -> true }
            }
            is PersistentOrderedSetBuilder<*> -> {
                hashMapBuilder.node.equalsWith(other.hashMapBuilder.node) { _, _ -> true }
            }
            else -> super.equals(other)
        }
    }

    /**
     * We provide [equals], so as a matter of style, we should also provide [hashCode].
     * However, the implementation from [AbstractMutableSet] is enough.
     */
    override fun hashCode(): Int = super<AbstractMutableSet>.hashCode()
}