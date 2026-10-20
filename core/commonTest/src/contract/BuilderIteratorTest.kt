/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
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
        val mapCalls = listOf<BuilderOperation<PersistentMap.Builder<Any?, String>>>(
            BuilderOperation("clear()") { clear() },
            BuilderOperation("putAll(an empty hash map)", throwsCME = false) { putAll(persistentHashMapOf()) },
            BuilderOperation("keys.retainAll(a hash set)", throwsCME = false) { keys.retainAll(persistentHashSetOf(1, 2)) },
            BuilderOperation("keys.removeAll(a hash set)", throwsCME = false) { keys.removeAll(persistentHashSetOf(1, 2)) },
        )
        val setCalls = listOf<BuilderOperation<PersistentSet.Builder<Any?>>>(
            BuilderOperation("clear()") { clear() },
            BuilderOperation("retainAll(a hash set)", throwsCME = false) { retainAll(persistentHashSetOf(1, 2)) },
            BuilderOperation("retainAll(the empty hash set)", throwsCME = false) { retainAll(persistentHashSetOf()) },
            BuilderOperation("retainAll(a hash set builder)", throwsCME = false) { retainAll(persistentHashSetOf(1, 2).builder()) },
            BuilderOperation("removeAll(a hash set)", throwsCME = false) { removeAll(persistentHashSetOf(1, 2)) },
            BuilderOperation("removeAll(a hash set builder)", throwsCME = false) { removeAll(persistentHashSetOf(1, 2).builder()) },
        )
        testIterator(emptyMapBuilders, mapViews, mapCalls, iteratorCalls)
        testIterator(emptySetBuilders, setViews, setCalls, iteratorCalls)
    }
}
