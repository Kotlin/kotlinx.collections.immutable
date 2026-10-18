/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `clear on an empty builder invalidates its live iterators`() = runCells {
        val clearList = BuilderOperation<PersistentList.Builder<Any>>("clear()") { clear() }
        val clearMap = BuilderOperation<PersistentMap.Builder<Any, String>>("clear()") { clear() }
        val clearSet = BuilderOperation<PersistentSet.Builder<Any>>("clear()") { clear() }
        for (iteratorOp in iteratorOperations) {
            testIterator(emptyListBuilders, listViews, clearList, iteratorOp)
            testIterator(emptyHashMapBuilders + emptyOrderedMapBuilders, mapViews, clearMap, iteratorOp)
            testIterator(emptyHashSetBuilders + emptyOrderedSetBuilders, setViews, clearSet, iteratorOp)
        }
    }

    @Test
    fun `hash map iterator after its own remove and a call that keeps the size`() = runCells {
        for (operation in mapOperations.filter { !it.throwsCME }) for ((origin, newBuilder) in hashMapBuilders) for ((view, open) in mapViews) {
            cell("$origin, $view, remove() then ${operation.description}") {
                for (returned in 1..newBuilder().size) {
                    val builder = newBuilder()
                    val order = builder.keys.toList()
                    val iterator = builder.open()
                    repeat(returned) { val _ = iterator.next() }
                    iterator.remove()

                    operation.function(builder)

                    val rest = order.drop(returned)
                    val expected = when (view) {
                        "entries" -> rest.map { it to builder[it] }
                        "keys" -> rest
                        else -> rest.map { builder[it] }
                    }
                    assertEquals(expected, iterator.drain(), "after $returned next")
                }
            }
        }
    }
}

private fun <B> Cells.sweep(builders: Origins<B>, views: Views<B, MutableIterator<*>>, operations: List<BuilderOperation<B>>) {
    for (operation in operations) for (iteratorOp in iteratorOperations) testIterator(builders, views, operation, iteratorOp)
}
