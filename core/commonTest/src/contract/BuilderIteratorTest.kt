/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlin.test.Test

/**
 * The builder iterators fail fast as java.util's do. A call through the builder that moves its size makes every call
 * on a live iterator throw ConcurrentModificationException, with IllegalStateException first for a remove or a set
 * when next returned nothing, and clear() counts as such a call even on an empty builder. After a call that keeps the
 * size, an overwrite or a no-op, a live iterator does what the iterator of a stdlib list of the builder's content does.
 */
class BuilderIteratorTest {

    @Test
    fun list() = runCells {
        for (operation in listOperations) {
            for (iteratorOp in iteratorOperations) testIterator(listBuilders, listViews, operation, iteratorOp)
            for (iteratorOp in listIteratorOperations) testIterator(listBuilders, listIteratorViews, operation, iteratorOp)
        }
    }

    @Test
    fun hashSet() = runCells { sweep(hashSetBuilders, setViews, setOperations) }

    @Test
    fun orderedSet() = runCells { sweep(orderedSetBuilders, setViews, setOperations) }

    @Test
    fun hashMap() = runCells { sweep(hashMapBuilders, mapViews, mapOperations) }

    @Test
    fun orderedMap() = runCells { sweep(orderedMapBuilders, mapViews, mapOperations) }

    @Test
    fun `calls on an empty map or set builder`() = runCells {
        val clearMap = BuilderOperation<PersistentMap.Builder<Any, String>>("clear()") { clear() }
        val setCalls = listOf<BuilderOperation<PersistentSet.Builder<Any>>>(
            BuilderOperation("clear()") { clear() },
            BuilderOperation("retainAll(a hash set)", throwsCME = false) { retainAll(persistentHashSetOf(1, 2)) },
            BuilderOperation("retainAll(the empty hash set)", throwsCME = false) { retainAll(persistentHashSetOf()) },
            BuilderOperation("retainAll(a hash set builder)", throwsCME = false) { retainAll(persistentHashSetOf(1, 2).builder()) },
            BuilderOperation("removeAll(a hash set)", throwsCME = false) { removeAll(persistentHashSetOf(1, 2)) },
            BuilderOperation("removeAll(a hash set builder)", throwsCME = false) { removeAll(persistentHashSetOf(1, 2).builder()) },
        )
        sweep(emptyHashMapBuilders + emptyOrderedMapBuilders, mapViews, listOf(clearMap))
        sweep(emptyHashSetBuilders + emptyOrderedSetBuilders, setViews, setCalls)
    }
}

private fun <B> Cells.sweep(builders: Origins<B>, views: Views<B, MutableIterator<*>>, operations: List<BuilderOperation<B>>) {
    for (operation in operations) for (iteratorOp in iteratorOperations) testIterator(builders, views, operation, iteratorOp)
}
