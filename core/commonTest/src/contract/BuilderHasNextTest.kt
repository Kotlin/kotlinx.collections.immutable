/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * hasNext of the builder iterators never throws. After a call through the builder it answers from the iterator's own
 * cursor, so that next reports the comodification: true until the iterator has returned as many elements as the
 * builder held when it was opened. The list builder's iterator answers from the live size, as ArrayList's.
 */
class BuilderHasNextTest {

    @Test
    fun list() = runCells { for (operation in listOperations) testHasNext(listBuilders, listViews, operation, followsTheSize = true) }

    @Test
    fun hashMap() = runCells { for (operation in hashMapOperations) testHasNext(hashMapBuilders, mapViews, operation) }

    @Test
    fun hashSet() = runCells { for (operation in hashSetOperations) testHasNext(hashSetBuilders, setViews, operation) }

    @Test
    fun orderedMap() = runCells { for (operation in orderedMapOperations) testHasNext(orderedMapBuilders, mapViews, operation) }

    @Test
    fun orderedSet() = runCells { for (operation in orderedSetOperations) testHasNext(orderedSetBuilders, setViews, operation) }

    @Test
    fun `add to an empty list builder during iteration turns hasNext true and next throws`() {
        val builder = persistentListOf<Int>().builder()
        val iterator = builder.iterator()
        assertFalse(iterator.hasNext())

        builder.add(1)

        assertTrue(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `hasPrevious after a clear stays true and previous throws`() = forEachBuilder(listBuildersOf(TAIL_ONLY)) { message, builder ->
        val iterator = builder.listIterator()
        repeat(2) { val _ = iterator.next() }

        builder.clear()

        assertTrue(iterator.hasPrevious(), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.previous() }
    }

    @Test
    fun `put on an empty map builder during iteration keeps hasNext false and next throws`() = runCells {
        val builders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>> = listOf(
            "an empty hash map builder" to { persistentHashMapOf<Any, String>().builder() },
            "an empty ordered map builder" to { persistentMapOf<Any, String>().builder() },
        )
        val put = BuilderOperation<PersistentMap.Builder<Any, String>>("put(new key)") { put("new", "n") }
        testHasNext(builders, mapViews, put)
        for (iteratorOp in iteratorOperations) testIterator(builders, mapViews, put, iteratorOp)
    }

    @Test
    fun `add to an empty set builder during iteration keeps hasNext false and next throws`() = runCells {
        val builders: List<Pair<String, () -> PersistentSet.Builder<Any>>> = listOf(
            "an empty hash set builder" to { persistentHashSetOf<Any>().builder() },
            "an empty ordered set builder" to { persistentSetOf<Any>().builder() },
        )
        val add = BuilderOperation<PersistentSet.Builder<Any>>("add(a new element)") { add("new") }
        testHasNext(builders, setViews, add)
        for (iteratorOp in iteratorOperations) testIterator(builders, setViews, add, iteratorOp)
    }

    @Test
    fun `remove of every element through the iterator of an ordered set builder ends the iteration`() = runCells {
        for (size in 1..3) testRemoveOfEveryElement(orderedSetBuildersOf(List<Any>(size) { it }), setViews)
        testRemoveOfEveryElement(orderedSetBuilders, setViews)
    }

    @Test
    fun `remove of every entry through the iterators of an ordered map builder ends the iteration`() = runCells {
        for (size in 1..3) testRemoveOfEveryElement(orderedMapBuildersOf(List<Pair<Any, String>>(size) { it to "v$it" }), mapViews)
        testRemoveOfEveryElement(orderedMapBuilders, mapViews)
    }
}

private fun <C : Any> Cells.testRemoveOfEveryElement(builders: List<Pair<String, () -> C>>, views: List<Pair<String, C.() -> MutableIterator<*>>>) {
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = sizeOf("$origin, $viewName", newBuilder, open) ?: continue
        cell("$origin of $size, $viewName, remove of every element") {
            val builder = newBuilder()
            val iterator = builder.open()
            repeat(size) {
                assertTrue(iterator.hasNext())
                val _ = iterator.next()
                iterator.remove()
            }

            assertEquals(emptyList(), builder.open().drain())
            assertFalse(iterator.hasNext())
            assertFailsWith<NoSuchElementException> { iterator.next() }
        }
    }
}

private fun <C : Any> Cells.testHasNext(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> MutableIterator<*>>>,
    builderOp: BuilderOperation<C>,
    /** true when hasNext answers from the live size, as the list builder's iterator does. */
    followsTheSize: Boolean = false,
) {
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = sizeOf("$origin, $viewName", newBuilder, open) ?: continue
        for (returned in 0..size) cell("$origin, $viewName, hasNext() after $returned next, ${builderOp.description}") {
            val builder = newBuilder()
            val iterator = builder.open()
            repeat(returned) { val _ = iterator.next() }

            builderOp.function.invoke(builder)

            val end = if (followsTheSize) builder.open().drain().size else size
            assertEquals(returned != end, iterator.hasNext())
        }
    }
}
