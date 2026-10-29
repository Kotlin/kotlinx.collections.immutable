/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

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
        testIterator(listBuilders, listViews, listOperations)
        testIterator(listBuilders, listIteratorViews, listOperations)
        testIterator(emptyListBuilders, listViews, emptyListCalls)
        testIterator(emptyListBuilders, listIteratorViews, emptyListCalls)
    }

    @Test
    fun hashSet() = runCells { testIterator(hashSetBuilders, nextOnly, setOperations) }

    @Test
    fun orderedSet() = runCells { testIterator(orderedSetBuilders, nextOnly, setOperations) }
}

private val nextOnly: Views<SetBuilder, MutableIterator<*>> = views(rows { "next()" { next() } }) { "iterator" { iterator() } }
