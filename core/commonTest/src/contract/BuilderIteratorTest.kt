/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The builder iterators fail fast as java.util's do. A call through the builder that moves its size makes next and
 * remove of every live iterator throw ConcurrentModificationException, with IllegalStateException first when next
 * returned nothing, and clear() counts as such a call even on an empty builder. A call that keeps the size, an
 * overwrite or a no-op, leaves them valid, and the iteration goes on over the live content.
 */
class BuilderIteratorTest {

    @Test
    fun hashSet() = runCells { sweep(hashSetBuilders, setViews, hashSetOperations) }

    @Test
    fun orderedSet() = runCells { sweep(orderedSetBuilders, setViews, orderedSetOperations) }

    @Test
    fun hashMap() = runCells { sweep(hashMapBuilders, mapViews, hashMapOperations) }

    @Test
    fun orderedMap() = runCells { sweep(orderedMapBuilders, mapViews, orderedMapOperations) }

    @Test
    fun `hash map iterator after its own remove and a call that keeps the size`() = runCells {
        for (operation in hashMapOperations.filter { !it.throwsCME }) testRemoveThenCall(hashMapBuilders, operation)
    }

    @Test
    fun `setValue of a map builder entry after a call that moved the size`() = runCells {
        for (operation in hashMapOperations.filter { it.throwsCME }) testSetValueAfter(hashMapBuilders, operation)
        for (operation in orderedMapOperations.filter { it.throwsCME }) testSetValueAfter(orderedMapBuilders, operation)
    }

    @Test
    fun `clear on an empty builder invalidates its live iterators`() {
        val maps = listOf("hash map" to persistentHashMapOf<Int, Int>().builder(), "ordered map" to persistentMapOf<Int, Int>().builder())
        for ((name, builder) in maps) {
            val iterators = listOf(builder.keys.iterator(), builder.values.iterator(), builder.entries.iterator())
            builder.clear()
            for (iterator in iterators) assertFailsWith<ConcurrentModificationException>(name) { iterator.next() }
        }
        val sets = listOf("hash set" to persistentHashSetOf<Int>().builder(), "ordered set" to persistentSetOf<Int>().builder())
        for ((name, builder) in sets) {
            val iterator = builder.iterator()
            builder.clear()
            assertFailsWith<ConcurrentModificationException>(name) { iterator.next() }
        }
    }
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

/** An entry's setValue after the structural [operation] writes if its key is there; the iterator stays failed. */
private fun Cells.testSetValueAfter(
    builders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>>,
    operation: BuilderOperation<PersistentMap.Builder<Any, String>>,
) {
    for ((origin, newBuilder) in builders) {
        for (position in 0..<newBuilder().size) cell("$origin, setValue of entry $position after ${operation.description}") {
            val builder = newBuilder()
            val iterator = builder.entries.iterator()
            repeat(position) { val _ = iterator.next() }
            val entry = iterator.next()
            val read = entry.value

            operation.function(builder)
            val kept = entry.key in builder

            assertEquals(read, entry.setValue("z"))
            assertEquals(if (kept) "z" else null, builder[entry.key])
            assertEquals(kept, entry.key in builder)
            assertFailsWith<ConcurrentModificationException> { iterator.next() }
        }
    }
}

/** After the iterator's own remove, a size-keeping [operation] leaves the rest of the iteration live and in order. */
private fun Cells.testRemoveThenCall(
    builders: List<Pair<String, () -> PersistentMap.Builder<Any, String>>>,
    operation: BuilderOperation<PersistentMap.Builder<Any, String>>,
) {
    for ((origin, newBuilder) in builders) for ((view, open) in mapViews) {
        for (returned in 1..newBuilder().size) cell("$origin, $view, remove() after $returned next and ${operation.description}") {
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
            assertEquals(expected, iterator.drain())
        }
    }
}
