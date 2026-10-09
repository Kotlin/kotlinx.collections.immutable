/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import kotlinx.collections.immutable.implementations.immutableSet.TrieNode
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashSet
import tests.IntWrapper
import tests.contract.BuilderOperation
import tests.contract.iteratorOperations
import tests.contract.testIterationContinues
import tests.contract.testIterator
import tests.trie.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Builders of every trie shape: from a set, which shares its nodes, and from adds, which own theirs. */
private val hashSetBuilders: List<Pair<String, () -> PersistentSet.Builder<Any>>> = trieShapes.flatMap { (shape, elements) ->
    listOf(
        "from a set of $shape" to { elements.toPersistentHashSet().builder() },
        "from adds of $shape" to { persistentHashSetOf<Any>().builder().apply { addAll(elements) } },
    )
}

/** Runs [test] on a builder of [elements] from a set, which shares its nodes, and from adds, which own theirs. */
private fun <E> forEachBuilder(elements: List<E>, test: (message: String, builder: PersistentSet.Builder<E>) -> Unit) {
    test("from a set", elements.toPersistentHashSet().builder())
    test("from adds", persistentHashSetOf<E>().builder().apply { addAll(elements) })
}

/** A set of [elements] as the implementation, for its node. */
private fun hashSet(vararg elements: IntWrapper): PersistentHashSet<IntWrapper> =
    persistentHashSetOf(*elements) as PersistentHashSet<IntWrapper>

/** A builder of [elements] from a set as the implementation, for its node and modCount. */
private fun hashSetBuilder(vararg elements: IntWrapper): PersistentHashSetBuilder<IntWrapper> =
    hashSet(*elements).builder() as PersistentHashSetBuilder<IntWrapper>

/** The elements of a receiver and of the persistent set merged into it, named by the trie shape the merge meets. */
private class MergeRow(val shape: String, val receiver: List<IntWrapper>, val argument: List<IntWrapper>)

@Suppress("UNCHECKED_CAST")
private fun collisionNodeOf(node: TrieNode<IntWrapper>): TrieNode<IntWrapper> {
    var current = node
    while (current.bitmap != 0) {
        current = current.buffer[0] as TrieNode<IntWrapper>
    }
    return current
}

class PersistentHashSetBuilderTest {

    @Test
    fun iterators() {
        val views = listOf<Pair<String, PersistentSet.Builder<Any>.() -> MutableIterator<*>>>("iterator" to { iterator() })
        val operations = listOf<BuilderOperation<PersistentSet.Builder<Any>>>(
            BuilderOperation("no call", throwsCME = false) { },

            BuilderOperation("add(a new element)") { add("new") },
            BuilderOperation("remove(the first element)") { remove(first()) },
            BuilderOperation("remove(the last element)") { remove(last()) },
            BuilderOperation("clear()") { clear() },
            BuilderOperation("addAll(a hash set with a new element)") { addAll(persistentHashSetOf("new")) },
            BuilderOperation("addAll(a list with a new element)") { addAll(listOf("new")) },
            BuilderOperation("removeAll(a hash set holding the first element)") { removeAll(persistentHashSetOf(first())) },
            BuilderOperation("removeAll(a list holding the first element)") { removeAll(listOf(first())) },
            BuilderOperation("retainAll(a hash set holding only the first element)") { retainAll(persistentHashSetOf(first())) },
            BuilderOperation("retainAll(a list holding only the first element)") { retainAll(listOf(first())) },
            BuilderOperation("add(a new element) then remove(it)") { add("new"); remove("new") },

            BuilderOperation("add(a stored element)", throwsCME = false) { add(first()) },
            BuilderOperation("remove(a missing element)", throwsCME = false) { remove("missing") },
            BuilderOperation("addAll(the built set)", throwsCME = false) { addAll(build()) },
            BuilderOperation("addAll(the stored elements as a list)", throwsCME = false) { addAll(toList()) },
            BuilderOperation("addAll(an equal hash set)", throwsCME = false) { addAll(toList().toPersistentHashSet()) },
            BuilderOperation("removeAll(a hash set of missing elements)", throwsCME = false) { removeAll(persistentHashSetOf("missing")) },
            BuilderOperation("removeAll(a list of missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
            BuilderOperation("retainAll(a superset as a hash set)", throwsCME = false) { retainAll((toList() + "missing").toPersistentHashSet()) },
            BuilderOperation("retainAll(a superset as a list)", throwsCME = false) { retainAll(toList() + "missing") },
        )

        for (operation in operations) {
            for (iteratorOp in iteratorOperations) testIterator(hashSetBuilders, views, operation, iteratorOp)
            if (!operation.throwsCME) testIterationContinues(hashSetBuilders, views, operation)
        }
    }

    @Test
    fun `retainAll and removeAll on an empty builder keep the iterator valid`() {
        val builders = listOf(
            "from the empty set" to { persistentHashSetOf<Int>().builder() },
            "emptied by remove" to { persistentHashSetOf(7).builder().apply { assertTrue(remove(7)) } },
        )
        val operations = listOf<Pair<String, MutableSet<Int>.() -> Boolean>>(
            "retainAll of a set" to { retainAll(persistentHashSetOf(1, 2)) },
            "retainAll of the empty set" to { retainAll(persistentHashSetOf<Int>()) },
            "retainAll of a builder" to { retainAll(persistentHashSetOf(1, 2).builder()) },
            "removeAll of a set" to { removeAll(persistentHashSetOf(1, 2)) },
            "removeAll of a builder" to { removeAll(persistentHashSetOf(1, 2).builder()) },
        )
        for ((flavour, newBuilder) in builders) {
            for ((operation, operate) in operations) {
                val builder = newBuilder()
                val iterator = builder.iterator()

                assertFalse(builder.operate(), "$operation, $flavour")

                assertFailsWith<NoSuchElementException>("$operation, $flavour") { iterator.next() }
            }
        }
    }

    @Test
    fun `retainAll and removeAll on a builder emptied by remove keep the built set`() {
        val operations = listOf<MutableSet<Int>.() -> Boolean>(
            { retainAll(persistentHashSetOf(1, 2)) },
            { removeAll(persistentHashSetOf(1, 2)) },
        )
        for (operate in operations) {
            val builder = persistentHashSetOf(7).builder()
            assertTrue(builder.remove(7))
            val built = builder.build()

            assertFalse(builder.operate())

            assertSame(built, builder.build())
        }
    }

    @Test
    fun `clear on an empty builder invalidates the iterator`() {
        val builder = persistentHashSetOf<Int>().builder()
        val iterator = builder.iterator()

        builder.clear()

        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `addAll of the stored element through the argument's subtree invalidates the iterator`() = forEachBuilder(listOf(collidingKey1)) { message, builder ->
        val iterator = builder.iterator()

        builder.addAll(persistentHashSetOf(collidingKey1.copy(), levelOneSibling))

        assertTrue(builder.contains(collidingKey1), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `removeAll of a stored element and retainAll that empties the builder invalidate the iterator`() = forEachBuilder(listOf(1, 2, 3)) { message, builder ->
        val iterator = builder.iterator()
        repeat(3) { val _ = iterator.next() }

        assertTrue(builder.removeAll(persistentHashSetOf(1)), message)

        assertEquals(2, builder.size, message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }

        val second = builder.iterator()

        assertTrue(builder.retainAll(persistentHashSetOf(8, 9)), message)

        assertEquals(0, builder.size, message)
        assertFailsWith<ConcurrentModificationException>(message) { second.next() }
    }

    @Test
    fun `add to the iterator of an empty builder keeps it false and next throws`() {
        val builder = persistentHashSetOf<Int>().builder()
        val iterator = builder.iterator()

        assertTrue(builder.add(1))

        assertFalse(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `removes of every remaining element keep hasNext true and next throws`() {
        for ((origin, newBuilder) in hashSetBuilders) {
            val builder = newBuilder()
            val elements = builder.toList()
            val iterator = builder.iterator()
            assertEquals(elements.take(2), List(2) { iterator.next() }, origin)

            for (element in elements) assertTrue(builder.remove(element), origin)

            assertTrue(iterator.hasNext(), origin)
            assertFailsWith<ConcurrentModificationException>(origin) { iterator.next() }
        }
    }

    @Test
    fun `addAll does not duplicate an element shared with a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, lastLevelSibling)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(collidingKey1, lastLevelSibling)))
        assertEquals(3, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, lastLevelSibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(3, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `addAll inserts a new element into a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3, lastLevelSibling)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(collidingKey3, lastLevelSibling)))
        assertEquals(4, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey3, lastLevelSibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(4, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `addAll keeps the receiver's instances and stores the argument's new ones`() {
        val rows = listOf(
            MergeRow("a collision node at the last level",
                listOf(collidingKey1, lastLevelSibling), listOf(collidingKey1.copy(), collidingKey2)),
            MergeRow("the receiver's collision node is an equality-subset of the argument's",
                listOf(collidingKey1, collidingKey2), listOf(collidingKey1.copy(), collidingKey2.copy(), collidingKey3)),
            MergeRow("the argument's subtree lacks the element",
                listOf(collidingKey1), listOf(levelOneSibling, otherLevelOneSibling)),
            MergeRow("the element collides with an argument element inside the subtree",
                listOf(collidingKey1), listOf(levelTwoSibling, levelOneSibling)),
        )
        for (row in rows) forEachBuilder(row.receiver) { origin, builder ->
            val message = "${row.shape}, $origin"
            val expected = (row.receiver + row.argument).distinct() // the receiver's instance of an element in both

            assertTrue(builder.addAll(row.argument.toPersistentHashSet()), message)

            assertEquals(expected.size, builder.size, message)
            for (element in expected) assertSame(element, builder.storedElement(element), "$message, $element")
        }
    }

    @Test
    fun `addAll reuses the argument's subtree holding the stored instance`() {
        val argument = hashSet(collidingKey1, levelOneSibling)
        val builder = hashSetBuilder(collidingKey1)

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll reuses the argument's collision node holding the stored instance`() {
        val argument = hashSet(collidingKey1, collidingKey2)
        val builder = hashSetBuilder(collidingKey1)

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll reuses the argument's collision node holding every stored instance`() {
        val argument = hashSet(collidingKey1, collidingKey2, collidingKey3)
        val builder = hashSetBuilder(collidingKey1, collidingKey2)

        builder.addAll(argument)

        assertEquals(3, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll leaves the argument its own instance`() {
        val argumentElement = collidingKey1.copy()
        val argument = persistentHashSetOf(argumentElement, levelOneSibling)
        val builder = persistentHashSetOf(collidingKey1).builder()

        builder.addAll(argument)

        assertSame(collidingKey1, builder.storedElement(collidingKey1))
        assertSame(argumentElement, argument.storedElement(argumentElement))
    }

    @Test
    fun `addAll counts one size change`() {
        val rows = listOf(
            MergeRow("the argument holds the element in a subtree",
                listOf(collidingKey1), listOf(collidingKey1.copy(), levelOneSibling)),
            MergeRow("the argument is disjoint",
                listOf(collidingKey1), listOf(levelOneSibling, otherLevelOneSibling)),
            MergeRow("the argument extends the collision node",
                listOf(collidingKey1, lastLevelSibling), listOf(collidingKey2, collidingKey3)),
        )
        for (row in rows) {
            val builder = hashSetBuilder(*row.receiver.toTypedArray())
            val modCount = builder.modCount

            assertTrue(builder.addAll(row.argument.toPersistentHashSet()), row.shape)

            assertEquals((row.receiver + row.argument).distinct().size, builder.size, row.shape)
            assertEquals(modCount + 1, builder.modCount, row.shape)
        }
    }

    @Test
    fun `retainAll promotes the only remaining element to the root`() {
        val builder = persistentHashSetOf(1, 33).builder() // 33 and 65 share root cell 1 with 1
        builder.retainAll(persistentHashSetOf(1, 65))
        val expected = persistentHashSetOf(1)

        assertTrue(expected.equals(builder))
        assertEquals(expected, builder.build())
        assertEquals(builder.build(), expected)
    }

    @Test
    fun `retainAll finds elements inside a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.retainAll(persistentHashSetOf(collidingKey1, lastLevelSibling)))
        assertEquals(1, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, lastLevelSibling).builder()
        assertTrue(reversedBuilder.retainAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `retainAll keeps the receiver's instances of the retained elements`() {
        val rows = listOf(
            MergeRow("a collision node at the last level",
                listOf(collidingKey1, collidingKey2, lastLevelSibling), listOf(collidingKey1.copy(), lastLevelSibling.copy())),
            MergeRow("another element at the argument's path in the receiver's subtree",
                listOf(collidingKey1, levelOneSibling, rootSibling), listOf(collidingKey3, rootSibling.copy())),
            MergeRow("no cell for the argument's element in the receiver's subtree",
                listOf(collidingKey1, levelOneSibling, rootSibling), listOf(otherLevelOneSibling, rootSibling.copy())),
            MergeRow("the receiver's collision node lacks the argument's element",
                listOf(collidingKey1, collidingKey2, lastLevelSibling), listOf(collidingKey3, lastLevelSibling)),
            MergeRow("the argument's collision node is an equality-subset of the receiver's",
                listOf(collidingKey1, collidingKey2, collidingKey3), listOf(collidingKey1.copy(), collidingKey2.copy())),
        )
        for (row in rows) forEachBuilder(row.receiver) { origin, builder ->
            val message = "${row.shape}, $origin"
            val expected = row.receiver.filter { it in row.argument }

            assertTrue(builder.retainAll(row.argument.toPersistentHashSet()), message)

            assertEquals(expected.size, builder.size, message)
            for (element in expected) assertSame(element, builder.storedElement(element), "$message, $element")
        }
    }

    @Test
    fun `retainAll reuses the argument's node holding the stored instance`() {
        val argument = hashSet(collidingKey1)
        val builder = hashSetBuilder(collidingKey1, levelOneSibling)

        builder.retainAll(argument)

        assertEquals(1, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `retainAll reuses the argument's collision node holding the stored instances`() {
        val argument = hashSet(collidingKey1, collidingKey2)
        val builder = hashSetBuilder(collidingKey1, collidingKey2, collidingKey3)

        builder.retainAll(argument)

        assertEquals(2, builder.size)
        assertSame(collisionNodeOf(argument.node), collisionNodeOf(builder.node))
    }

    @Test
    fun `removeAll removes elements stored in a bottom-level collision node`() {
        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.removeAll(persistentHashSetOf(collidingKey1, lastLevelSibling)))
        assertEquals(1, builder.size)
        assertEquals(persistentHashSetOf(collidingKey2), builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, lastLevelSibling).builder()
        assertTrue(reversedBuilder.removeAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(persistentHashSetOf(lastLevelSibling), reversedBuilder.build())
    }

    @Test
    fun `addAll keeps the stored element instances`() {
        val random = Random(316)
        repeat(200) { iteration ->
            val receiverElements = randomTrieKeys(random, 0..<14)
            val argumentElements = randomTrieKeys(random, 0..<14)
            val builder = persistentHashSetOf<IntWrapper>().builder().apply { addAll(receiverElements.values) }

            builder.addAll(argumentElements.values.toPersistentHashSet())

            val shape = "iteration $iteration, receiver ${receiverElements.keys}, argument ${argumentElements.keys}"
            // The receiver's instance of an id in both; not `argument + receiver`, whose putAll keeps an equal old value on JS and Wasm.
            val expected = (receiverElements.keys + argumentElements.keys).associateWith { receiverElements[it] ?: argumentElements.getValue(it) }
            assertEquals(expected.size, builder.size, shape)
            for ((id, element) in expected) assertSame(element, builder.storedElement(element), "$shape, id $id")
        }
    }

    @Test
    fun `retainAll keeps the stored element instances`() {
        val random = Random(322)
        repeat(200) { iteration ->
            val receiverElements = randomTrieKeys(random, 0..<21)
            val argumentElements = randomTrieKeys(random, 0..<21)
            val builder = persistentHashSetOf<IntWrapper>().builder().apply { addAll(receiverElements.values) }

            builder.retainAll(argumentElements.values.toPersistentHashSet())

            val shape = "iteration $iteration, receiver ${receiverElements.keys}, argument ${argumentElements.keys}"
            val expected = receiverElements.filterKeys { it in argumentElements }
            assertEquals(expected.size, builder.size, shape)
            for ((id, element) in expected) assertSame(element, builder.storedElement(element), "$shape, id $id")
        }
    }
}
