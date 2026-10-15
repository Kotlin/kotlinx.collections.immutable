/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.Test

/**
 * The builder iterators fail fast as java.util's do. A call through the builder that moves its size makes next and
 * remove of every live iterator throw ConcurrentModificationException, with IllegalStateException first when next
 * returned nothing, and clear() counts as such a call even on an empty builder. A call that keeps the size, an
 * overwrite or a no-op, leaves them valid, and the iteration goes on over the live content.
 */
class BuilderIteratorTest {

    @Test
    fun list() = runCells {
        sweep(listBuilders, listViews, listOperations)
        for (operation in listOperations) {
            for (iteratorOp in listIteratorOperations) testIterator(listBuilders, listIteratorViews, operation, iteratorOp)
        }
    }

    @Test
    fun hashSet() = runCells { sweep(hashSetBuilders, setViews, hashSetOperations) }

    @Test
    fun orderedSet() = runCells { sweep(orderedSetBuilders, setViews, orderedSetOperations) }
}

private fun <C : Any> Cells.sweep(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> MutableIterator<*>>>,
    operations: List<BuilderOperation<C>>,
) {
    for (operation in operations) {
        for (iteratorOp in iteratorOperations) testIterator(builders, views, operation, iteratorOp)
        if (!operation.throwsCME) testIterationContinues(builders, views, operation)
    }
}
