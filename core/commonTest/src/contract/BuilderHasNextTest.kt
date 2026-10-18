/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import tests.trie.MutableKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * hasNext of the builder iterators never throws. After a call through the builder it answers from the iterator's own
 * cursor, so that next reports the comodification: true until the iterator has returned as many elements as the
 * builder held when it was opened. The list builder's iterator answers from the live size, as ArrayList's.
 *
 * The iterators of an ordered map or set and of its builder follow a chain of keys. A key whose hash changed after it
 * was stored makes next and remove throw ConcurrentModificationException, and the iteration goes on from that key once
 * the hash is back. A key made equal to another one makes the chain loop or end early, and the next that notices it
 * throws ConcurrentModificationException and ends the iteration.
 */
class BuilderHasNextTest {

    @Test
    fun list() = runCells { for (operation in listOperations) testHasNext(listBuilders, listViews, operation) }

    @Test
    fun hashMap() = runCells { for (operation in mapOperations) testHasNext(hashMapBuilders, mapViews, operation) }

    @Test
    fun hashSet() = runCells { for (operation in setOperations) testHasNext(hashSetBuilders, setViews, operation) }

    @Test
    fun orderedMap() = runCells { for (operation in mapOperations) testHasNext(orderedMapBuilders, mapViews, operation) }

    @Test
    fun orderedSet() = runCells { for (operation in setOperations) testHasNext(orderedSetBuilders, setViews, operation) }

    @Test
    fun `add or put on an empty builder during iteration makes next throw`() = runCells {
        val emptyMapBuilders = emptyHashMapBuilders + emptyOrderedMapBuilders
        val emptySetBuilders = emptyHashSetBuilders + emptyOrderedSetBuilders
        val addToList = BuilderOperation<PersistentList.Builder<Any>>("add()") { add("new") }
        val put = BuilderOperation<PersistentMap.Builder<Any, String>>("put(new key)") { put("new", "n") }
        val addToSet = BuilderOperation<PersistentSet.Builder<Any>>("add(a new element)") { add("new") }
        val next = IteratorOperation<MutableIterator<*>>("next()") { next() }
        testHasNext(emptyListBuilders, listViews, addToList)
        testHasNext(emptyMapBuilders, mapViews, put)
        testHasNext(emptySetBuilders, setViews, addToSet)
        testIterator(emptyListBuilders, listViews, addToList, next)
        testIterator(emptyMapBuilders, mapViews, put, next)
        testIterator(emptySetBuilders, setViews, addToSet, next)
    }

    @Test
    fun `hasPrevious after a clear stays true and previous throws`() = forEachBuilder(listBuildersOf(TAIL_ONLY)) { builder ->
        val iterator = builder.listIterator()
        repeat(2) { val _ = iterator.next() }

        builder.clear()

        assertTrue(iterator.hasPrevious())
        assertFailsWith<ConcurrentModificationException> { iterator.previous() }
    }

    @Test
    fun `next after the hash of the next key changed throws ConcurrentModificationException and goes on from that key once the hash is back`() = runCells {
        for ((name, viewOf) in orderedViews) cell(name) {
            val key = MutableKey(2)
            val keys = listOf(MutableKey(1), key, MutableKey(3))
            val iterator = viewOf(keys).iterator()
            val _ = iterator.next()

            key.hash = 9
            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            key.hash = 2

            assertEquals(viewOf(keys).iterator().drain().drop(1), iterator.drain())
        }
    }

    @Test
    fun `a key made equal to an earlier one makes the last next throw ConcurrentModificationException`() = runCells {
        for ((name, viewOf) in orderedViews) cell(name) {
            val key = MutableKey(3)
            val iterator = viewOf(listOf(MutableKey(1), MutableKey(2), key)).iterator()
            key.id = 1
            repeat(2) { val _ = iterator.next() }

            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `a key made equal to a later one makes the next that ends the chain early throw ConcurrentModificationException`() = runCells {
        for ((name, viewOf) in orderedViews) cell(name) {
            val key = MutableKey(2)
            val iterator = viewOf(listOf(MutableKey(1), key, MutableKey(3))).iterator()
            key.id = 3
            val _ = iterator.next()

            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `remove through the iterator of a key whose hash changed after next throws ConcurrentModificationException`() = runCells {
        for ((name, viewOf) in orderedBuilderViews) cell(name) {
            val key = MutableKey(2)
            val view = viewOf(listOf(MutableKey(1), key, MutableKey(3)))
            val iterator = view.iterator()
            repeat(2) { val _ = iterator.next() }
            key.hash = 4

            assertFailsWith<ConcurrentModificationException> { iterator.remove() }
            assertEquals(3, view.size)
        }
    }
}

private fun <B> Cells.testHasNext(builders: Origins<B>, views: Views<B, MutableIterator<*>>, builderOp: BuilderOperation<B>) {
    for ((origin, newBuilder) in builders) for ((view, open) in views) {
        cell("$origin, $view, hasNext() after ${builderOp.description}") {
            val size = newBuilder().open().drain().size
            for (returned in 0..size) at("after $returned next") {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }

                builderOp.function(builder)

                val end = if (builder is List<*>) builder.size else size
                assertEquals(returned != end, iterator.hasNext())
            }
        }
    }
}

private fun orderedMapOf(keys: List<MutableKey>) = keys.associateWith { "v${it.id}" }.toPersistentMap()

private val orderedBuilderViews: List<Pair<String, (List<MutableKey>) -> MutableCollection<*>>> = listOf(
    "the ordered map builder's entries" to { keys -> orderedMapOf(keys).builder().entries },
    "the ordered map builder's keys" to { keys -> orderedMapOf(keys).builder().keys },
    "the ordered map builder's values" to { keys -> orderedMapOf(keys).builder().values },
    "the ordered set builder" to { keys -> keys.toPersistentSet().builder() },
)

private val orderedViews: List<Pair<String, (List<MutableKey>) -> Collection<*>>> = orderedBuilderViews + listOf(
    "the ordered map's entries" to { keys -> orderedMapOf(keys).entries },
    "the ordered map's keys" to { keys -> orderedMapOf(keys).keys },
    "the ordered map's values" to { keys -> orderedMapOf(keys).values },
    "the ordered set" to { keys -> keys.toPersistentSet() },
)
