/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.Test

/**
 * The builder iterators fail fast as java.util's do. A call through the builder that moves its size makes next of
 * every live iterator throw ConcurrentModificationException. After a call that keeps the size, an overwrite or a no-op,
 * next does what next of a stdlib list of the builder's content does.
 */
class BuilderIteratorTest {

    @Test
    fun hashSet() = runCells { sweep(hashSetBuilders, setViews, setOperations) }

    @Test
    fun orderedSet() = runCells { sweep(orderedSetBuilders, setViews, setOperations) }
}

private val next = IteratorOperation<MutableIterator<*>>("next()") { next() }

private fun <B> Cells.sweep(builders: Origins<B>, views: Views<B, MutableIterator<*>>, operations: List<BuilderOperation<B>>) {
    for (operation in operations) testIterator(builders, views, operation, next)
}
