/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlin.test.Test

/**
 * The builder iterators fail fast as java.util's do. A call through the builder that moves its size, or a clear() even
 * of an empty builder, freezes a live iterator where the call found it: every call on it throws
 * ConcurrentModificationException, except a remove or a set when next returned nothing, which throws
 * IllegalStateException. After a call that keeps the size, an overwrite or a no-op, a live iterator does what the
 * iterator of a stdlib list of the builder's content does.
 *
 * A new call through a builder is a row of [listOperations], [mapOperations] or [setOperations]; a new call on a live
 * iterator is a row of [iteratorCalls] or [listIteratorCalls].
 */
class BuilderIteratorTest {

    /** The list iterator also takes previous, add and set; its own add and remove do not freeze it, as in ArrayList. */
    @Test
    fun list() = runCells {
        val emptyListCalls = operations<ListBuilder>(
            thatKeepTheSize = { "no call" { } },
            thatMoveTheSize = { "clear()" { clear() } },
        )
        testIterator(listBuilders, listViews, listOperations, iteratorCalls)
        testIterator(listBuilders, listIteratorViews, listOperations, listIteratorCalls)
        testIterator(emptyListBuilders, listViews, emptyListCalls, iteratorCalls)
        testIterator(emptyListBuilders, listIteratorViews, emptyListCalls, listIteratorCalls)
    }

    @Test
    fun hashSet() = runCells { testIterator(hashSetBuilders, setViews, setOperations, iteratorCalls) }

    @Test
    fun orderedSet() = runCells { testIterator(orderedSetBuilders, setViews, setOperations, iteratorCalls) }

    @Test
    fun hashMap() = runCells { testIterator(hashMapBuilders, mapViews, mapOperations, iteratorCalls) }

    @Test
    fun orderedMap() = runCells { testIterator(orderedMapBuilders, mapViews, mapOperations, iteratorCalls) }

    @Test
    fun `clear of an empty map or set builder freezes its iterators and calls that change nothing do not`() = runCells {
        val mapCalls = operations<MapBuilder>(
            thatKeepTheSize = {
                "no call" { }
                "putAll(an empty hash map)" { putAll(persistentHashMapOf()) }
                "keys.retainAll(a hash set)" { keys.retainAll(persistentHashSetOf(1, 2)) }
                "keys.removeAll(a hash set)" { keys.removeAll(persistentHashSetOf(1, 2)) }
            },
            thatMoveTheSize = { "clear()" { clear() } },
        )
        val setCalls = operations<SetBuilder>(
            thatKeepTheSize = {
                "no call" { }
                "retainAll(a hash set)" { retainAll(persistentHashSetOf(1, 2)) }
                "retainAll(the empty hash set)" { retainAll(persistentHashSetOf()) }
                "retainAll(a hash set builder)" { retainAll(persistentHashSetOf(1, 2).builder()) }
                "removeAll(a hash set)" { removeAll(persistentHashSetOf(1, 2)) }
                "removeAll(a hash set builder)" { removeAll(persistentHashSetOf(1, 2).builder()) }
            },
            thatMoveTheSize = { "clear()" { clear() } },
        )
        testIterator(emptyMapBuilders, mapViews, mapCalls, iteratorCalls)
        testIterator(emptySetBuilders, setViews, setCalls, iteratorCalls)
    }
}
