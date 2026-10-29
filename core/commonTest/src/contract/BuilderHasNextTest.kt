/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import tests.trie.MutableKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * hasNext of the builder iterators never throws. After a call through the builder it answers from the iterator's own
 * cursor, so that next reports the comodification: true until the iterator has returned as many elements as the
 * builder held when it was opened. The list builder's iterator answers hasNext from the live size, and hasPrevious,
 * nextIndex and previousIndex from the cursor alone, as ArrayList's.
 *
 * The iterators of an ordered map or set and of its builder follow a chain of keys. A key whose hash changed after it
 * was stored makes next and remove throw ConcurrentModificationException, and the iteration goes on from that key once
 * the hash is back. A key made equal to another one makes the chain loop or end early, and the next that notices it
 * throws ConcurrentModificationException and ends the iteration.
 *
 * A new call through a builder is a row of [listOperations], [mapOperations] or [setOperations]; a new scenario of a
 * key changed after it was stored is a cell over [orderedCollections] or [orderedBuilderCollections].
 */
class BuilderHasNextTest {

    @Test
    fun list() = runCells { testHasNext(listBuilders, listIteratorViews.openers, listOperations) }

    @Test
    fun hashMap() = runCells { testHasNext(hashMapBuilders, mapViews.openers, mapOperations) }

    @Test
    fun hashSet() = runCells { testHasNext(hashSetBuilders, setViews.openers, setOperations) }

    @Test
    fun orderedMap() = runCells { testHasNext(orderedMapBuilders, mapViews.openers, mapOperations) }

    @Test
    fun orderedSet() = runCells { testHasNext(orderedSetBuilders, setViews.openers, setOperations) }

    @Test
    fun `next throws until the changed hash of the next key is back`() = runCells {
        for ((name, collectionOf) in orderedCollections) cell(name) {
            val key = MutableKey(2)
            val keys = listOf(MutableKey(1), key, MutableKey(3))
            val iterator = collectionOf(keys).iterator()
            val _ = iterator.next()

            key.hash = 9
            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            key.hash = 2

            assertEquals(collectionOf(keys).iterator().drain().drop(1), iterator.drain())
        }
    }

    @Test
    fun `a key made equal to an earlier one makes the last next throw`() = runCells {
        for ((name, collectionOf) in orderedCollections) cell(name) {
            val key = MutableKey(3)
            val iterator = collectionOf(listOf(MutableKey(1), MutableKey(2), key)).iterator()
            key.id = 1
            repeat(2) { val _ = iterator.next() }

            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `a key made equal to a later one makes the next that ends the chain early throw`() = runCells {
        for ((name, collectionOf) in orderedCollections) cell(name) {
            val key = MutableKey(2)
            val iterator = collectionOf(listOf(MutableKey(1), key, MutableKey(3))).iterator()
            key.id = 3
            val _ = iterator.next()

            assertFailsWith<ConcurrentModificationException> { iterator.next() }
            assertFalse(iterator.hasNext())
        }
    }

    @Test
    fun `remove of a key whose hash changed after next returned it throws and takes nothing out`() = runCells {
        for ((name, collectionOf) in orderedBuilderCollections) cell(name) {
            val key = MutableKey(2)
            val collection = collectionOf(listOf(MutableKey(1), key, MutableKey(3)))
            val iterator = collection.iterator()
            repeat(2) { val _ = iterator.next() }
            key.hash = 4

            assertFailsWith<ConcurrentModificationException> { iterator.remove() }
            assertEquals(3, collection.size)
        }
    }
}

private fun <B> Cells.testHasNext(
    builders: Origins<B>,
    openers: List<Pair<String, B.() -> MutableIterator<*>>>,
    operations: List<BuilderOperation<B>>,
) {
    for ((origin, newBuilder) in builders) for ((view, open) in openers) for (operation in operations) {
        cell("$origin, $view after ${operation.description}") {
            val opened = newBuilder().open().drain()
            for (returned in 0..opened.size) at("after $returned next") {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }

                operation.function(builder)

                val end = if (builder is List<*>) builder.size else opened.size
                at("hasNext()") { assertEquals(returned != end, iterator.hasNext()) }
                if (iterator is ListIterator<*>) {
                    at("hasPrevious()") { assertEquals(returned > 0, iterator.hasPrevious()) }
                    at("nextIndex()") { assertEquals(returned, iterator.nextIndex()) }
                    at("previousIndex()") { assertEquals(returned - 1, iterator.previousIndex()) }
                }
                if (operation.movesTheSize) assertFailsWith<ConcurrentModificationException>("next()") { iterator.next() }
            }
        }
    }
}

private fun orderedMapOf(keys: List<MutableKey>) = keys.associateWith { "v${it.id}" }.toPersistentMap()

private val orderedBuilderCollections: List<Pair<String, (List<MutableKey>) -> MutableCollection<*>>> = rows {
    "the ordered map builder's entries" { keys -> orderedMapOf(keys).builder().entries }
    "the ordered map builder's keys" { keys -> orderedMapOf(keys).builder().keys }
    "the ordered map builder's values" { keys -> orderedMapOf(keys).builder().values }
    "the ordered set builder" { keys -> keys.toPersistentSet().builder() }
}

private val orderedCollections: List<Pair<String, (List<MutableKey>) -> Collection<*>>> = orderedBuilderCollections + rows {
    "the ordered map's entries" { keys -> orderedMapOf(keys).entries }
    "the ordered map's keys" { keys -> orderedMapOf(keys).keys }
    "the ordered map's values" { keys -> orderedMapOf(keys).values }
    "the ordered set" { keys -> keys.toPersistentSet() }
}
